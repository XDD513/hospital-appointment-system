package com.hospital.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * OSS配置类
 *
 * @author Hospital Team
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssConfig {

    /**
     * AccessKey ID
     */
    private String accessKeyId;

    /**
     * AccessKey Secret
     */
    private String accessKeySecret;

    /**
     * OSS端点
     */
    private String endpoint;

    /**
     * 存储桶名称
     */
    private String bucketName;

    /**
     * 头像存储路径
     */
    private String avatarPath = "avatar/";

    /**
     * URL协议（http或https）
     */
    private String urlProtocol = "https";
}

