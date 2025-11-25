package com.hospital.dto.request;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户设置请求DTO
 *
 * @author Hospital Team
 */
@Data
public class UserSettingsRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 消息通知开关（true-开启 false-关闭）
     */
    private Boolean notification;

    /**
     * 短信提醒开关（true-开启 false-关闭）
     */
    private Boolean smsReminder;

    /**
     * 预约提醒开关（医生端：新预约时通知）
     */
    private Boolean appointmentReminder;

    /**
     * 评价通知开关（医生端：新评价时通知）
     */
    private Boolean reviewNotification;

    /**
     * 操作提醒开关（管理员端：重要操作前提醒）
     */
    private Boolean operationReminder;
}

