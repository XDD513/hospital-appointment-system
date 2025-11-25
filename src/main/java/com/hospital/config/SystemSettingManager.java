package com.hospital.config;

import com.hospital.entity.SystemConfig;
import com.hospital.mapper.SystemConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 系统配置管理器。
 *
 * 该组件会在应用启动时一次性拉取 system_config 表中的所有配置，
 * 并缓存在内存中；当管理员在后台更新配置后，可通过 refresh 方法
 * 立即刷新缓存，保证配置修改后可以即时在全局范围内生效。
 */
@Slf4j
@Component
public class SystemSettingManager {

    private final SystemConfigMapper systemConfigMapper;
    private final Map<String, String> settingsCache = new ConcurrentHashMap<>();

    public SystemSettingManager(SystemConfigMapper systemConfigMapper) {
        this.systemConfigMapper = systemConfigMapper;
    }

    @PostConstruct
    public void init() {
        refresh();
    }

    /**
     * 重新加载所有系统配置。
     * 使用同步方法避免并发刷新时缓存数据不一致。
     */
    public synchronized void refresh() {
        List<SystemConfig> configs = systemConfigMapper.selectList(null);
        if (CollectionUtils.isEmpty(configs)) {
            log.warn("system_config 表为空，未能加载到任何配置项");
            settingsCache.clear();
            return;
        }

        Map<String, String> latest = new ConcurrentHashMap<>(configs.size());
        for (SystemConfig config : configs) {
            if (!StringUtils.hasText(config.getConfigKey())) {
                continue;
            }
            latest.put(config.getConfigKey(), config.getConfigValue());
        }

        settingsCache.clear();
        settingsCache.putAll(latest);
        log.info("系统配置已刷新，共加载 {} 项配置", settingsCache.size());
    }

    /**
     * 获取字符串配置。
     */
    public String getString(String key, String defaultValue) {
        String value = settingsCache.get(key);
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    /**
     * 获取布尔配置。
     */
    public Boolean getBoolean(String key, Boolean defaultValue) {
        String value = settingsCache.get(key);
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }

    /**
     * 获取整数配置。
     */
    public Integer getInteger(String key, Integer defaultValue) {
        String value = settingsCache.get(key);
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return Integer.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            log.warn("配置项 {} 的值 {} 无法转换为整数，使用默认值 {}", key, value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 获取不可变配置快照，便于调试或对外暴露。
     */
    public Map<String, String> snapshot() {
        return Collections.unmodifiableMap(new ConcurrentHashMap<>(settingsCache));
    }
}

