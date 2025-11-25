package com.hospital.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 区域API配置类
 * 从application.yml中读取阿里云区域API的配置信息
 *
 * @author Hospital Team
 * @since 2025-12-19
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "aliyun.area.api")
public class AreaConfig {

    /**
     * API地址（从配置文件读取，但通常使用AreaConstants中的常量）
     */
    private String host;

    /**
     * API路径（从配置文件读取，但通常使用AreaConstants中的常量）
     */
    private String path;

    /**
     * 应用Key（用于签名认证）
     */
    private String appKey;

    /**
     * 应用Secret（用于签名认证）
     */
    private String appSecret;

    /**
     * 应用Code（用于简单身份认证）
     */
    private String appCode;

    /**
     * 缓存TTL（秒），默认7天
     */
    private Long cacheTtlSeconds = 604800L;

    /**
     * 缓存配置（保留用于向后兼容）
     */
    private CacheConfig cache = new CacheConfig();

    @Data
    public static class CacheConfig {
        /**
         * 缓存前缀
         */
        private String prefix;

        /**
         * 缓存TTL（秒）
         */
        private Long ttlSeconds;
    }
}

