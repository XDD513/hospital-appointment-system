package com.hospital.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 医患对话消息实体
 */
@Data
@TableName("conversation_message")
public class ConversationMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    @TableField("conversation_id")
    private Long conversationId;

    @TableField("sender_role")
    private String senderRole;

    @TableField("sender_id")
    private Long senderId;

    @TableField("sender_name")
    private String senderName;

    @TableField("sender_avatar")
    private String senderAvatar;

    @TableField("content")
    private String content;

    @TableField("content_type")
    private String contentType;

    @TableField("attachment_url")
    private String attachmentUrl;

    @TableField("metadata")
    private String metadata;

    @TableField("is_read")
    private Integer isRead;

    @TableField("read_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readAt;

    @TableField(value = "sent_at", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sentAt;

    @TableField("deleted")
    private Integer deleted;
}


