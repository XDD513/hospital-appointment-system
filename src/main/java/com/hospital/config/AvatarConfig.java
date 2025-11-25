package com.hospital.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 头像配置类
 * 从application.yml中读取头像相关的配置信息
 *
 * @author Hospital Team
 * @since 2025-12-19
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "hospital.avatar")
public class AvatarConfig {
    /**
     * 默认患者头像URL
     */
    private String defaultPatient;

    /**
     * 默认医生头像URL
     */
    private String defaultDoctor;

    /**
     * 默认管理员头像URL
     */
    private String defaultAdmin;

    /**
     * 头像TTL（分钟）
     */
    private Integer ttlMinutes = 60;
}

