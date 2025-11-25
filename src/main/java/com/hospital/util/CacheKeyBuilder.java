package com.hospital.util;

import java.util.Map;
import java.util.TreeMap;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 缓存键构建器
 * 统一缓存键的构建逻辑，避免重复的字符串拼接
 *
 * @author Hospital Team
 * @since 2025-11-25
 */
public class CacheKeyBuilder {

    private static final String SEPARATOR = ":";
    private final StringBuilder keyBuilder;

    private CacheKeyBuilder(String prefix) {
        this.keyBuilder = new StringBuilder(prefix);
    }

    /**
     * 创建缓存键构建器
     *
     * @param prefix 键前缀
     * @return CacheKeyBuilder实例
     */
    public static CacheKeyBuilder of(String prefix) {
        return new CacheKeyBuilder(prefix);
    }

    /**
     * 添加键段
     *
     * @param segment 键段
     * @return 当前构建器实例
     */
    public CacheKeyBuilder append(String segment) {
        if (segment != null && !segment.isEmpty()) {
            keyBuilder.append(SEPARATOR).append(segment);
        }
        return this;
    }

    /**
     * 添加键段（带键名）
     *
     * @param key   键名
     * @param value 值
     * @return 当前构建器实例
     */
    public CacheKeyBuilder append(String key, Object value) {
        if (value != null) {
            keyBuilder.append(SEPARATOR).append(key).append(SEPARATOR).append(value);
        }
        return this;
    }

    /**
     * 添加分页参数
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @return 当前构建器实例
     */
    public CacheKeyBuilder appendPage(Integer page, Integer pageSize) {
        keyBuilder.append(":p").append(page != null ? page : 1);
        keyBuilder.append(":s").append(pageSize != null ? pageSize : 10);
        return this;
    }

    /**
     * 添加参数哈希（用于复杂参数）
     *
     * @param params 参数Map
     * @return 当前构建器实例
     */
    public CacheKeyBuilder appendParamsHash(Map<String, Object> params) {
        if (params != null && !params.isEmpty()) {
            String hash = generateParamsHash(params);
            keyBuilder.append(":h:").append(hash);
        }
        return this;
    }

    /**
     * 构建最终的缓存键
     *
     * @return 缓存键字符串
     */
    public String build() {
        return keyBuilder.toString();
    }

    /**
     * 生成参数哈希值（MD5前8位）
     *
     * @param params 参数Map
     * @return 8位十六进制哈希值
     */
    private static String generateParamsHash(Map<String, Object> params) {
        try {
            // 使用TreeMap保证参数顺序一致
            TreeMap<String, Object> sortedParams = new TreeMap<>(params);

            // 构建参数字符串
            StringBuilder paramStr = new StringBuilder();
            for (Map.Entry<String, Object> entry : sortedParams.entrySet()) {
                if (entry.getValue() != null) {
                    paramStr.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
                }
            }

            // 生成MD5哈希
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(paramStr.toString().getBytes(StandardCharsets.UTF_8));

            // 取前8位（16个字符的十六进制）
            StringBuilder hashStr = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                hashStr.append(String.format("%02x", hashBytes[i]));
            }

            return hashStr.toString();
        } catch (Exception e) {
            // 降级方案：使用简单的hashCode
            return String.valueOf(params.hashCode()).replace("-", "n");
        }
    }
}

