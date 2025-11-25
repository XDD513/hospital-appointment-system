package com.hospital.dto.response;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户设置响应DTO
 *
 * @author Hospital Team
 */
@Data
public class UserSettingsResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息通知开关
     */
    private Boolean notification = true;

    /**
     * 短信提醒开关
     */
    private Boolean smsReminder = true;

    /**
     * 预约提醒开关（医生端）
     */
    private Boolean appointmentReminder = true;

    /**
     * 评价通知开关（医生端）
     */
    private Boolean reviewNotification = true;

    /**
     * 操作提醒开关（管理员端）
     */
    private Boolean operationReminder = true;
}

