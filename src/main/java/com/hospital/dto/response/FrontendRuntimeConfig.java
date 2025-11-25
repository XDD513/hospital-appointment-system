package com.hospital.dto.response;

import lombok.Data;

/**
 * 提供给前端的运行时配置 DTO。
 */
@Data
public class FrontendRuntimeConfig {

    /**
     * API 基础地址（可带域名或代理前缀）。
     */
    private String apiBaseUrl;

    /**
     * WebSocket 地址。
     */
    private String wsBaseUrl;

    /**
     * axios 请求超时时间（毫秒）。
     */
    private Integer requestTimeout;

    /**
     * 消息提示持续时间（毫秒）。
     */
    private Integer messageDuration;

    /**
     * 默认头像配置。
     */
    private AvatarConfig defaultAvatars;

    /**
     * 系统基础信息（名称、版本等）。
     */
    private SystemInfo systemInfo;

    /**
     * 预约相关基础参数。
     */
    private BasicConfig basic;

    /**
     * 通知设置。
     */
    private NotificationConfig notification;

    /**
     * 安全设置。
     */
    private SecurityConfig security;

    /**
     * 邮件设置。
     */
    private EmailConfig email;

    @Data
    public static class AvatarConfig {
        private String patient;
        private String doctor;
        private String system;
    }

    @Data
    public static class SystemInfo {
        /**
         * 系统名称
         */
        private String name;

        /**
         * 系统版本
         */
        private String version;

        /**
         * 是否处于维护模式
         */
        private Boolean maintenanceMode;
    }

    @Data
    public static class BasicConfig {
        private Integer advanceDays;
        private Integer cancelHours;
        private Integer paymentTimeout;
    }

    @Data
    public static class NotificationConfig {
        private Boolean appointmentReminder;
        private Integer reminderHours;
        private Boolean smsEnabled;
        private Boolean emailEnabled;
        private Boolean systemEnabled;
    }

    @Data
    public static class SecurityConfig {
        private Integer minPasswordLength;
        private Boolean loginLockEnabled;
        private Integer maxLoginAttempts;
        private Integer lockDurationMinutes;
        private Integer sessionTimeoutMinutes;
    }

    @Data
    public static class EmailConfig {
        private String smtpHost;
        private Integer smtpPort;
        private String fromEmail;
        private Boolean sslEnabled;
    }
}

