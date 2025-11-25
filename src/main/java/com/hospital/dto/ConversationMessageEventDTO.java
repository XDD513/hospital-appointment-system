package com.hospital.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 会话消息推送事件，用于通过 RabbitMQ 广播并由 WebSocket 推送给双方。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessageEventDTO implements Serializable {

    private Long conversationId;
    private Long messageId;
    private Long patientId; // 向后兼容
    private Long doctorId; // 向后兼容
    private Long doctorUserId; // 向后兼容

    // 新字段：支持三种身份
    private String conversationType;
    private Long participant1UserId;
    private String participant1Role;
    private Long participant2UserId;
    private String participant2Role;

    private Long senderId;

    private String senderRole;
    private String senderName;
    private String senderAvatar;

    private String content;
    private String contentType;
    private LocalDateTime sentAt;
    private String lastMessagePreview;
}


