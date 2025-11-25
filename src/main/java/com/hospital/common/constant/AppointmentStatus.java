package com.hospital.common.constant;

import lombok.Getter;

/**
 * 预约状态枚举
 *
 * @author Hospital Team
 * @since 2025-10-24
 */
@Getter
public enum AppointmentStatus {

    /**
     * 待支付
     */
    PENDING_PAYMENT("PENDING_PAYMENT", "待支付"),

    /**
     * 待就诊
     */
    PENDING_VISIT("PENDING_VISIT", "待就诊"),

    /**
     * 已确认
     */
    CONFIRMED("CONFIRMED", "已确认"),

    /**
     * 就诊中
     */
    IN_PROGRESS("IN_PROGRESS", "就诊中"),

    /**
     * 已完成
     */
    COMPLETED("COMPLETED", "已完成"),

    /**
     * 已取消
     */
    CANCELLED("CANCELLED", "已取消"),

    /**
     * 爽约
     */
    NO_SHOW("NO_SHOW", "爽约");

    private final String code;
    private final String desc;

    AppointmentStatus(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 根据编码获取枚举
     */
    public static AppointmentStatus getByCode(String code) {
        for (AppointmentStatus status : values()) {
            if (status.getCode().equals(code)) {
                return status;
            }
        }
        return null;
    }
}

