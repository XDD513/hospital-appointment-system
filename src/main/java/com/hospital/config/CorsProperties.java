package com.hospital.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * CORS配置类
 * 从application.yml中读取CORS相关的配置信息
 *
 * @author Hospital Team
 * @since 2025-12-19
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "hospital.cors")
public class CorsProperties {
    /**
     * 允许的源（多个用逗号分隔）
     */
    private String allowedOrigins = "http://localhost:3000";

    /**
     * 允许的方法（多个用逗号分隔）
     */
    private String allowedMethods = "GET,POST,PUT,DELETE,OPTIONS";

    /**
     * 允许的请求头
     */
    private String allowedHeaders = "*";

    /**
     * 是否允许携带凭证
     */
    private Boolean allowCredentials = true;

    /**
     * 获取允许的源列表
     */
    public List<String> getAllowedOriginsList() {
        return Arrays.asList(allowedOrigins.split(","));
    }

    /**
     * 获取允许的方法列表
     */
    public List<String> getAllowedMethodsList() {
        return Arrays.asList(allowedMethods.split(","));
    }
}

