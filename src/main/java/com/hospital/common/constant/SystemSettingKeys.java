package com.hospital.common.constant;

/**
 * 系统配置键常量。
 * 统一管理 system_config 表中使用到的配置Key，避免魔法字符串散落在各处。
 */
public final class SystemSettingKeys {

    private SystemSettingKeys() {
        // 工具类禁止实例化
    }

    // ==================== 基础设置 ====================
    public static final String SYSTEM_NAME = "system.name";
    public static final String SYSTEM_VERSION = "system.version";
    public static final String SYSTEM_MAINTENANCE_MODE = "system.maintenance_mode";

    // ==================== 预约 & 支付 ====================
    public static final String APPOINTMENT_ADVANCE_DAYS = "appointment.advance_days";
    public static final String APPOINTMENT_CANCEL_HOURS = "appointment.cancel_hours";
    public static final String PAYMENT_TIMEOUT_MINUTES = "payment.timeout";

    // ==================== 通知设置 ====================
    public static final String NOTIFICATION_APPOINTMENT_REMINDER = "notification.appointment_reminder";
    public static final String NOTIFICATION_REMINDER_HOURS = "notification.reminder_hours";
    public static final String NOTIFICATION_SMS_ENABLED = "notification.sms_enabled";
    public static final String NOTIFICATION_EMAIL_ENABLED = "notification.email_enabled";
    public static final String NOTIFICATION_SYSTEM_ENABLED = "notification.system_enabled";

    // ==================== 安全设置 ====================
    public static final String SECURITY_MIN_PASSWORD_LENGTH = "security.min_password_length";
    public static final String SECURITY_LOGIN_LOCK_ENABLED = "security.login_lock_enabled";
    public static final String SECURITY_MAX_LOGIN_ATTEMPTS = "security.max_login_attempts";
    public static final String SECURITY_LOCK_DURATION = "security.lock_duration";
    public static final String SECURITY_SESSION_TIMEOUT = "security.session_timeout";

    // ==================== 邮件设置 ====================
    public static final String EMAIL_SMTP_HOST = "email.smtp_host";
    public static final String EMAIL_SMTP_PORT = "email.smtp_port";
    public static final String EMAIL_FROM = "email.from_email";
    public static final String EMAIL_PASSWORD = "email.password";
    public static final String EMAIL_SSL_ENABLED = "email.ssl_enabled";
}

