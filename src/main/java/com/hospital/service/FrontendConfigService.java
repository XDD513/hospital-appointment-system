package com.hospital.service;

import com.hospital.common.constant.SystemSettingKeys;
import com.hospital.config.FrontendConfigProperties;
import com.hospital.config.SystemSettingManager;
import com.hospital.dto.response.FrontendRuntimeConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 将 Nacos 中的前端配置转换为运行时 DTO。
 */
@Service
@RequiredArgsConstructor
public class FrontendConfigService {

    private final FrontendConfigProperties frontendConfigProperties;
    private final SystemSettingManager systemSettingManager;

    /**
     * 获取前端运行时配置。
     */
    public FrontendRuntimeConfig getRuntimeConfig() {
        FrontendRuntimeConfig runtimeConfig = new FrontendRuntimeConfig();
        runtimeConfig.setApiBaseUrl(resolveOrDefault(frontendConfigProperties.getApiBaseUrl(), "/api"));
        runtimeConfig.setWsBaseUrl(resolveOrDefault(frontendConfigProperties.getWsBaseUrl(), "/ws"));
        runtimeConfig.setRequestTimeout(
                frontendConfigProperties.getRequestTimeout() != null
                        ? frontendConfigProperties.getRequestTimeout()
                        : 10000);
        runtimeConfig.setMessageDuration(
                frontendConfigProperties.getMessageDuration() != null
                        ? frontendConfigProperties.getMessageDuration()
                        : 3000);

        FrontendRuntimeConfig.AvatarConfig avatarConfig = new FrontendRuntimeConfig.AvatarConfig();
        FrontendConfigProperties.Avatars props = frontendConfigProperties.getDefaultAvatars();
        avatarConfig.setPatient(props.getPatient());
        avatarConfig.setDoctor(props.getDoctor());
        avatarConfig.setSystem(props.getSystem());
        runtimeConfig.setDefaultAvatars(avatarConfig);

        FrontendRuntimeConfig.SystemInfo systemInfo = new FrontendRuntimeConfig.SystemInfo();
        systemInfo.setName(systemSettingManager.getString(SystemSettingKeys.SYSTEM_NAME, "中医体质辨识系统"));
        systemInfo.setVersion(systemSettingManager.getString(SystemSettingKeys.SYSTEM_VERSION, "v1.0.0"));
        systemInfo.setMaintenanceMode(systemSettingManager.getBoolean(SystemSettingKeys.SYSTEM_MAINTENANCE_MODE, Boolean.FALSE));
        runtimeConfig.setSystemInfo(systemInfo);

        FrontendRuntimeConfig.BasicConfig basicConfig = new FrontendRuntimeConfig.BasicConfig();
        basicConfig.setAdvanceDays(systemSettingManager.getInteger(SystemSettingKeys.APPOINTMENT_ADVANCE_DAYS, 7));
        basicConfig.setCancelHours(systemSettingManager.getInteger(SystemSettingKeys.APPOINTMENT_CANCEL_HOURS, 2));
        basicConfig.setPaymentTimeout(systemSettingManager.getInteger(SystemSettingKeys.PAYMENT_TIMEOUT_MINUTES, 30));
        runtimeConfig.setBasic(basicConfig);

        FrontendRuntimeConfig.NotificationConfig notificationConfig = new FrontendRuntimeConfig.NotificationConfig();
        notificationConfig.setAppointmentReminder(systemSettingManager.getBoolean(SystemSettingKeys.NOTIFICATION_APPOINTMENT_REMINDER, Boolean.TRUE));
        notificationConfig.setReminderHours(systemSettingManager.getInteger(SystemSettingKeys.NOTIFICATION_REMINDER_HOURS, 2));
        notificationConfig.setSmsEnabled(systemSettingManager.getBoolean(SystemSettingKeys.NOTIFICATION_SMS_ENABLED, Boolean.FALSE));
        notificationConfig.setEmailEnabled(systemSettingManager.getBoolean(SystemSettingKeys.NOTIFICATION_EMAIL_ENABLED, Boolean.FALSE));
        notificationConfig.setSystemEnabled(systemSettingManager.getBoolean(SystemSettingKeys.NOTIFICATION_SYSTEM_ENABLED, Boolean.TRUE));
        runtimeConfig.setNotification(notificationConfig);

        FrontendRuntimeConfig.SecurityConfig securityConfig = new FrontendRuntimeConfig.SecurityConfig();
        securityConfig.setMinPasswordLength(systemSettingManager.getInteger(SystemSettingKeys.SECURITY_MIN_PASSWORD_LENGTH, 6));
        securityConfig.setLoginLockEnabled(systemSettingManager.getBoolean(SystemSettingKeys.SECURITY_LOGIN_LOCK_ENABLED, Boolean.FALSE));
        securityConfig.setMaxLoginAttempts(systemSettingManager.getInteger(SystemSettingKeys.SECURITY_MAX_LOGIN_ATTEMPTS, 5));
        securityConfig.setLockDurationMinutes(systemSettingManager.getInteger(SystemSettingKeys.SECURITY_LOCK_DURATION, 15));
        securityConfig.setSessionTimeoutMinutes(systemSettingManager.getInteger(SystemSettingKeys.SECURITY_SESSION_TIMEOUT, 120));
        runtimeConfig.setSecurity(securityConfig);

        FrontendRuntimeConfig.EmailConfig emailConfig = new FrontendRuntimeConfig.EmailConfig();
        emailConfig.setSmtpHost(systemSettingManager.getString(SystemSettingKeys.EMAIL_SMTP_HOST, ""));
        emailConfig.setSmtpPort(systemSettingManager.getInteger(SystemSettingKeys.EMAIL_SMTP_PORT, 587));
        emailConfig.setFromEmail(systemSettingManager.getString(SystemSettingKeys.EMAIL_FROM, ""));
        emailConfig.setSslEnabled(systemSettingManager.getBoolean(SystemSettingKeys.EMAIL_SSL_ENABLED, Boolean.TRUE));
        runtimeConfig.setEmail(emailConfig);

        return runtimeConfig;
    }

    private String resolveOrDefault(String value, String defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return value;
    }
}

