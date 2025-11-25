package com.hospital.messaging;

import com.hospital.config.RabbitMQConfig;
import com.hospital.dto.ConsultationReminderMessageDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 医生接诊完成提醒消息发布者。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConsultationReminderPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final RabbitMQConfig rabbitMQConfig;

    public void publish(ConsultationReminderMessageDTO message) {
        try {
            log.info("准备发送接诊完成提醒消息：exchange={}, routingKey={}, appointmentId={}, patientId={}",
                    rabbitMQConfig.getConsultationExchange(),
                    rabbitMQConfig.getConsultationRoutingKey(),
                    message.getAppointmentId(),
                    message.getPatientId());
            rabbitTemplate.convertAndSend(
                    rabbitMQConfig.getConsultationExchange(),
                    rabbitMQConfig.getConsultationRoutingKey(),
                    message
            );
            log.info("发送接诊完成提醒消息成功：appointmentId={}, patientId={}",
                    message.getAppointmentId(), message.getPatientId());
        } catch (Exception e) {
            log.error("发送接诊完成提醒消息失败：appointmentId={}, patientId={}, error={}",
                    message.getAppointmentId(), message.getPatientId(), e.getMessage(), e);
        }
    }
}

