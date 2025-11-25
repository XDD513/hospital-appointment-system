package com.hospital.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 推送给前端的通知消息。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationMessageDTO {

    private Long id;
    private String title;
    private String content;
    private String type;
    private Integer readStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

