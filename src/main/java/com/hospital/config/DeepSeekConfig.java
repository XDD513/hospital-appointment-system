package com.hospital.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * DeepSeek API配置类
 * 从application.yml中读取DeepSeek API的配置信息
 *
 * @author Hospital Team
 * @since 2025-01-XX
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "aliyun.deepseek")
public class DeepSeekConfig {

    /**
     * API密钥
     */
    private String apiKey;

    /**
     * API地址
     */
    private String apiUrl = "https://api.deepseek.com";

    /**
     * 模型名称
     */
    private String model = "deepseek-chat";

    /**
     * 最大token数
     */
    private Integer maxTokens = 500;

    /**
     * 温度参数（0-2，控制随机性）
     */
    private Double temperature = 0.7;

    /**
     * 缓存TTL（小时）
     */
    private Integer cacheTtlHours = 2;
}

