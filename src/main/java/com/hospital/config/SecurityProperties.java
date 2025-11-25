package com.hospital.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 安全相关配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "hospital.security")
public class SecurityProperties {

    /**
     * AES密钥（16/24/32位），用于敏感数据加密
     */
    private String sensitiveKey = "Hospit@lAESKey2025";
}


