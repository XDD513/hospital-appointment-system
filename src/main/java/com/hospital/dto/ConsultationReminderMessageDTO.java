package com.hospital.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 医生完成接诊后发送给 RabbitMQ 的提醒消息体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsultationReminderMessageDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 预约ID
     */
    private Long appointmentId;

    /**
     * 患者ID
     */
    private Long patientId;

    /**
     * 医生姓名
     */
    private String doctorName;

    /**
     * 科室/分类名称
     */
    private String categoryName;

    /**
     * 提醒内容
     */
    private String message;

    /**
     * 消息创建时间
     */
    private LocalDateTime createdAt;
}

