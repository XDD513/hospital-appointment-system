package com.hospital.service;

import com.alibaba.cloud.nacos.NacosConfigManager;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.ConfigType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hospital.config.SystemSettingSyncProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.TreeMap;

/**
 * 将系统配置同步到 Nacos。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemSettingNacosSyncService {

    private final NacosConfigManager nacosConfigManager;
    private final ObjectMapper objectMapper;
    private final SystemSettingSyncProperties syncProperties;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    /**
     * 将当前系统配置同步到 Nacos。
     *
     * @param settings 最新的配置快照
     */
    public void sync(Map<String, String> settings) {
        if (!syncProperties.isEnabled()) {
            return;
        }
        if (CollectionUtils.isEmpty(settings)) {
            log.warn("系统配置为空，跳过 Nacos 同步");
            return;
        }
        try {
            String content = buildContent(settings);
            String dataId = resolveDataId();
            ConfigService configService = nacosConfigManager.getConfigService();
            boolean published = configService.publishConfig(
                    dataId,
                    syncProperties.getNacosGroup(),
                    content,
                    ConfigType.JSON.getType()
            );
            if (published) {
                log.info("系统配置已同步到 Nacos，dataId={}，group={}，条目数={}",
                        dataId, syncProperties.getNacosGroup(), settings.size());
            } else {
                log.warn("系统配置同步到 Nacos 失败（publishConfig 返回 false），dataId={}", dataId);
            }
        } catch (Exception e) {
            log.error("同步系统配置到 Nacos 失败", e);
        }
    }

    private String resolveDataId() {
        String pattern = syncProperties.getDataIdPattern();
        if (StringUtils.hasText(pattern)) {
            if (pattern.contains("%s")) {
                return String.format(pattern, activeProfile);
            }
            return pattern;
        }
        return "hospital-system-settings-" + activeProfile + ".json";
    }

    private String buildContent(Map<String, String> settings) throws JsonProcessingException {
        Map<String, Object> payload = new TreeMap<>();
        for (Map.Entry<String, String> entry : settings.entrySet()) {
            payload.put(entry.getKey(), convertValue(entry.getValue()));
        }
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
    }

    private Object convertValue(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String trimmed = value.trim();
        if ("true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed)) {
            return Boolean.parseBoolean(trimmed);
        }
        try {
            if (trimmed.contains(".")) {
                return Double.parseDouble(trimmed);
            }
            return Long.parseLong(trimmed);
        } catch (NumberFormatException ignored) {
        }
        return trimmed;
    }
}

