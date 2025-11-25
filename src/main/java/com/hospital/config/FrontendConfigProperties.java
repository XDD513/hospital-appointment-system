package com.hospital.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * 前端运行时配置，来源于 Nacos 配置中心。
 */
@Data
@Component
@RefreshScope
@ConfigurationProperties(prefix = "frontend")
public class FrontendConfigProperties {

    /**
     * 后端 API 基础地址，例如 http://localhost:8080 或 https://api.example.com。
     */
    private String apiBaseUrl = "/api";

    /**
     * WebSocket 基础地址。
     */
    private String wsBaseUrl = "/ws";

    /**
     * HTTP 请求超时时间（毫秒）。
     */
    private Integer requestTimeout = 10000;

    /**
     * 消息提示持续时间（毫秒）。
     */
    private Integer messageDuration = 3000;

    /**
     * 默认头像配置。
     */
    private Avatars defaultAvatars = new Avatars();

    @Data
    public static class Avatars {
        private String patient = "https://api.dicebear.com/7.x/thumbs/svg?seed=patient";
        private String doctor = "https://api.dicebear.com/7.x/thumbs/svg?seed=doctor";
        private String system = "https://api.dicebear.com/7.x/shapes/svg?seed=assistant";
    }
}

