package com.hospital.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.entity.UserNotification;
import com.hospital.dto.ConsultationReminderMessageDTO;
import com.hospital.dto.NotificationMessageDTO;
import com.hospital.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 接收接诊完成提醒消息并落库为用户通知。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConstitutionReminderListener {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = "${hospital.rabbitmq.consultation.queue:hospital.consultation.completed.queue}")
    public void onConsultationCompleted(ConsultationReminderMessageDTO message) {
        try {
            if (message == null || message.getPatientId() == null) {
                log.warn("收到的接诊提醒消息无效：{}", message);
                return;
            }

            log.info("接收到接诊完成提醒消息：appointmentId={}, patientId={}",
                    message.getAppointmentId(), message.getPatientId());

            UserNotification notification = new UserNotification();
            notification.setUserId(message.getPatientId());
            notification.setTitle("体质测试提醒");
            notification.setContent(message.getMessage());
            notification.setType("CONSULTATION_REMINDER");
            notification.setReadStatus(0);

            // 额外信息写入 metadata
            notification.setMetadata(objectMapper.writeValueAsString(message));
            notification.setCreatedAt(LocalDateTime.now());
            notification.setUpdatedAt(LocalDateTime.now());

            notificationService.createConstitutionReminder(notification);
            log.info("已保存体质测试提醒通知：notificationId={}, userId={}",
                    notification.getId(), notification.getUserId());

            NotificationMessageDTO payload = NotificationMessageDTO.builder()
                    .id(notification.getId())
                    .title(notification.getTitle())
                    .content(notification.getContent())
                    .type(notification.getType())
                    .readStatus(notification.getReadStatus())
                    .createdAt(notification.getCreatedAt())
                    .updatedAt(notification.getUpdatedAt())
                    .build();

            messagingTemplate.convertAndSendToUser(
                    String.valueOf(notification.getUserId()),
                    "/queue/notifications",
                    payload
            );
        } catch (Exception e) {
            log.error("处理接诊完成提醒消息失败：{}", message, e);
        }
    }
}

