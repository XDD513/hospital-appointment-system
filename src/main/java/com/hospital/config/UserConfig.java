package com.hospital.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 用户配置类
 * 从application.yml中读取用户相关的配置信息
 *
 * @author Hospital Team
 * @since 2025-12-19
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "hospital.user")
public class UserConfig {
    /**
     * 默认用户密码（用于新用户创建和密码重置）
     */
    private String defaultPassword = "123456";
}

