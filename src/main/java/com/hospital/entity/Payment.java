package com.hospital.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 支付记录实体类
 *
 * @author Hospital Team
 * @since 2025-10-24
 */
@Data
@TableName("payment")
public class Payment implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 支付ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 预约ID
     */
    @TableField("appointment_id")
    private Long appointmentId;

    /**
     * 接诊记录ID
     */
    @TableField("consultation_record_id")
    private Long consultationRecordId;

    /**
     * 患者ID
     */
    @TableField("patient_id")
    private Long patientId;

    /**
     * 支付类型（REGISTRATION-挂号 CONSULTATION-诊疗 PRESCRIPTION-处方）
     */
    @TableField("payment_type")
    private String paymentType;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 支付方式（CASH-现金 ALIPAY-支付宝 WECHAT-微信 CARD-银行卡）
     */
    @TableField("payment_method")
    private String paymentMethod;

    /**
     * 支付状态（PENDING-待支付 SUCCESS-成功 FAILED-失败 REFUNDED-已退款）
     */
    @TableField("payment_status")
    private String paymentStatus;

    /**
     * 交易流水号
     */
    @TableField("transaction_id")
    private String transactionId;

    /**
     * 支付时间
     */
    @TableField("paid_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime paidAt;

    /**
     * 退款时间
     */
    @TableField("refunded_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime refundedAt;

    /**
     * 退款原因
     */
    @TableField("refund_reason")
    private String refundReason;

    /**
     * 创建时间
     */
    @TableField("created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}

