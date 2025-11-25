package com.hospital.messaging;

import com.hospital.dto.ConversationMessageEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * 会话消息事件监听器，消费 RabbitMQ 消息并通过 WebSocket 推送给双方。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ConversationMessageListener {

    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = "${hospital.rabbitmq.conversation.queue:hospital.conversation.message.queue}")
    public void onConversationMessage(ConversationMessageEventDTO event) {
        if (event == null || event.getConversationId() == null) {
            log.warn("收到空的会话消息事件：{}", event);
            return;
        }
        log.info("收到会话消息事件：conversationId={}, messageId={}, senderRole={}, conversationType={}",
                event.getConversationId(), event.getMessageId(), event.getSenderRole(), event.getConversationType());

        String senderRole = event.getSenderRole() != null ? event.getSenderRole().toUpperCase() : "";

        // 获取发送者的用户ID
        Long senderUserId = getSenderUserId(event, senderRole);

        // 优先使用新字段（participant1UserId和participant2UserId）
        Long participant1UserId = event.getParticipant1UserId();
        Long participant2UserId = event.getParticipant2UserId();

        if (participant1UserId != null && participant2UserId != null && senderUserId != null) {
            // 使用新字段推送消息
            Long receiverUserId = null;

            // 确定接收者：如果发送者是participant1，接收者是participant2；反之亦然
            if (participant1UserId.equals(senderUserId)) {
                receiverUserId = participant2UserId;
            } else if (participant2UserId.equals(senderUserId)) {
                receiverUserId = participant1UserId;
            } else {
                log.warn("发送者用户ID与participant不匹配，无法确定接收者：senderUserId={}, participant1UserId={}, participant2UserId={}",
                        senderUserId, participant1UserId, participant2UserId);
            }

            // 推送给接收者
            if (receiverUserId != null) {
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(receiverUserId),
                        "/queue/conversations",
                        event
                );
                log.info("消息已推送给接收者：receiverUserId={}, senderUserId={}, senderRole={}, conversationType={}",
                        receiverUserId, senderUserId, senderRole, event.getConversationType());
            } else {
                log.warn("无法确定接收者，跳过WebSocket推送：senderUserId={}, participant1UserId={}, participant2UserId={}",
                        senderUserId, participant1UserId, participant2UserId);
            }

            // 推送给发送者自己（用于实时显示自己发送的消息）
            messagingTemplate.convertAndSendToUser(
                    String.valueOf(senderUserId),
                    "/queue/conversations",
                    event
            );

            return;
        }

        // 向后兼容：如果新字段为空，使用旧逻辑

        // 判断是否是管理员对话：通过conversationType或doctorId等于doctorUserId判断
        boolean isAdminConversation = "ADMIN_USER".equals(event.getConversationType())
                || (event.getDoctorId() != null && event.getDoctorUserId() != null
                        && event.getDoctorId().equals(event.getDoctorUserId()));

        if ("ADMIN".equals(senderRole)) {
            // 管理员发送的消息
            // 推送给接收者（patientId或participant1UserId）
            Long receiverUserId = event.getParticipant1UserId() != null ? event.getParticipant1UserId() : event.getPatientId();
            if (receiverUserId != null) {
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(receiverUserId),
                        "/queue/conversations",
                        event
                );
            }
            // 推送给管理员自己（用于实时显示自己发送的消息）
            Long adminUserId = event.getDoctorUserId() != null ? event.getDoctorUserId() : event.getSenderId();
            if (adminUserId != null) {
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(adminUserId),
                        "/queue/conversations",
                        event
                );
            }
        } else if (isAdminConversation) {
            // 管理员对话中，非管理员发送的消息（可能是患者或医生）
            // 推送给管理员（doctorUserId或participant2UserId）
            Long adminUserId = event.getParticipant2UserId() != null ? event.getParticipant2UserId() : event.getDoctorUserId();
            if (adminUserId != null) {
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(adminUserId),
                        "/queue/conversations",
                        event
                );
            }
            // 推送给发送者自己（用于实时显示自己发送的消息）
            // 对于医生，需要获取用户ID；对于患者，senderId就是用户ID
            Long senderUser = senderUserId != null ? senderUserId :
                    ("DOCTOR".equals(senderRole) ? event.getDoctorUserId() : event.getSenderId());
            if (senderUser != null) {
                messagingTemplate.convertAndSendToUser(
                        String.valueOf(senderUser),
                        "/queue/conversations",
                        event
                );
            }
        } else {
            // 普通医生-患者对话
            if ("DOCTOR".equals(senderRole)) {
                // 医生发送的消息，推送给患者（patientId）
                if (event.getPatientId() != null) {
                    messagingTemplate.convertAndSendToUser(
                            String.valueOf(event.getPatientId()),
                            "/queue/conversations",
                            event
                    );
                }
                // 推送给医生自己（用于实时显示自己发送的消息）
                if (event.getDoctorUserId() != null) {
                    messagingTemplate.convertAndSendToUser(
                            String.valueOf(event.getDoctorUserId()),
                            "/queue/conversations",
                            event
                    );
                }
            } else if ("PATIENT".equals(senderRole)) {
                // 患者发送的消息，推送给医生（doctorUserId）
                if (event.getDoctorUserId() != null) {
                    messagingTemplate.convertAndSendToUser(
                            String.valueOf(event.getDoctorUserId()),
                            "/queue/conversations",
                            event
                    );
                }
                // 推送给患者自己（用于实时显示自己发送的消息）
                if (event.getPatientId() != null) {
                    messagingTemplate.convertAndSendToUser(
                            String.valueOf(event.getPatientId()),
                            "/queue/conversations",
                            event
                    );
                }
            }
        }
    }

    /**
     * 获取发送者的用户ID
     */
    private Long getSenderUserId(ConversationMessageEventDTO event, String senderRole) {
        if ("PATIENT".equals(senderRole) || "ADMIN".equals(senderRole)) {
            // 患者和管理员：senderId就是用户ID
            return event.getSenderId();
        } else if ("DOCTOR".equals(senderRole)) {
            // 医生：senderId是医生实体ID，需要从事件中获取用户ID
            // 优先从participant信息中获取
            if (event.getParticipant1Role() != null && "DOCTOR".equals(event.getParticipant1Role())
                    && event.getParticipant1UserId() != null) {
                return event.getParticipant1UserId();
            }
            if (event.getParticipant2Role() != null && "DOCTOR".equals(event.getParticipant2Role())
                    && event.getParticipant2UserId() != null) {
                return event.getParticipant2UserId();
            }
            // 向后兼容：使用doctorUserId
            return event.getDoctorUserId();
        }
        return null;
    }
}


