package com.hospital.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 系统设置同步相关配置。
 */
@Data
@Component
@ConfigurationProperties(prefix = "hospital.system-settings.sync")
public class SystemSettingSyncProperties {

    /**
     * 是否启用同步到 Nacos 的功能。
     */
    private boolean enabled = true;

    /**
     * 同步到 Nacos 使用的分组，默认沿用后端分组。
     */
    private String nacosGroup = "HOSPITAL_BACKEND";

    /**
     * dataId 模板，支持使用 %s 占位当前 profile。
     */
    private String dataIdPattern = "hospital-system-settings-%s.json";
}

