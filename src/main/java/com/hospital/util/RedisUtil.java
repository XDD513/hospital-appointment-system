package com.hospital.util;

import com.hospital.common.constant.SystemConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import org.springframework.data.redis.core.ZSetOperations;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

/**
 * Redis工具类
 *
 * @author Hospital Team
 * @since 2025-10-24
 */
@Slf4j
@Component
public class RedisUtil {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 设置缓存
     *
     * @param key 键
     * @param value 值
     */
    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
        } catch (Exception e) {
            log.error("⚠️ Redis SET失败: key={}, error={}", key, e.getMessage(), e);
            throw new RuntimeException("Redis操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 设置缓存并设置过期时间
     *
     * @param key 键
     * @param value 值
     * @param timeout 过期时间
     * @param unit 时间单位
     */
    public void set(String key, Object value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
        } catch (Exception e) {
            log.error("⚠️ Redis SET失败: key={}, error={}", key, e.getMessage(), e);
            throw new RuntimeException("Redis操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取缓存
     *
     * @param key 键
     * @return 值
     */
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 删除缓存
     *
     * @param key 键
     * @return true-删除成功 false-删除失败
     */
    public Boolean delete(String key) {
        try {
            Boolean result = redisTemplate.delete(key);
            return result;
        } catch (Exception e) {
            log.error("⚠️ Redis DELETE失败: key={}, error={}", key, e.getMessage(), e);
            throw new RuntimeException("Redis操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 按模式删除缓存键（使用 keys 模式匹配，谨慎使用）
     * 例如：deleteByPattern("doctor:list:dept:*")
     *
     * @param pattern 键模式（可包含通配符 *）
     * @return 删除的键数量
     */
    public Long deleteByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys == null || keys.isEmpty()) {
                return 0L;
            }
            Long count = redisTemplate.delete(keys);
            return count;
        } catch (Exception e) {
            log.error("⚠️ Redis DELETE BY PATTERN失败: pattern={}, error={}", pattern, e.getMessage(), e);
            throw new RuntimeException("Redis操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 判断键是否存在
     *
     * @param key 键
     * @return true-存在 false-不存在
     */
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 设置过期时间
     *
     * @param key 键
     * @param timeout 过期时间
     * @param unit 时间单位
     * @return true-设置成功 false-设置失败
     */
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        return redisTemplate.expire(key, timeout, unit);
    }

    /**
     * 获取过期时间
     *
     * @param key 键
     * @return 过期时间（秒）
     */
    public Long getExpire(String key) {
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

    /**
     * 递增
     *
     * @param key 键
     * @param delta 递增步长
     * @return 递增后的值
     */
    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    /**
     * 递减
     *
     * @param key 键
     * @param delta 递减步长
     * @return 递减后的值
     */
    public Long decrement(String key, long delta) {
        return redisTemplate.opsForValue().decrement(key, delta);
    }

    /**
     * 尝试获取分布式锁
     *
     * @param key 锁的key
     * @param value 锁的value
     * @param timeout 超时时间
     * @param unit 时间单位
     * @return true-获取成功 false-获取失败
     */
    public Boolean tryLock(String key, String value, long timeout, TimeUnit unit) {
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
    }

    /**
     * 释放分布式锁
     *
     * @param key 锁的key
     */
    public void unlock(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 生成简化的缓存键（使用参数哈希值）
     * 将复杂的参数组合压缩成一个短的哈希字符串，避免层级过深
     *
     * 示例：
     * 原键：hospital:admin:user:list:page:1:size:10:search:ALL:role:ALL:status:ALL
     * 新键：hospital:admin:user:list:p1:s10:h:a1b2c3d4
     *
     * @param prefix 缓存键前缀（如：hospital:admin:user:list）
     * @param page 页码
     * @param pageSize 每页大小
     * @param params 其他参数Map（会生成哈希值）
     * @return 简化后的缓存键
     */
    public String buildCacheKey(String prefix, Integer page, Integer pageSize, Map<String, Object> params) {
        StringBuilder keyBuilder = new StringBuilder(prefix);

        // 分页参数直接显示（常用且重要）
        keyBuilder.append(":p").append(page != null ? page : 1);
        keyBuilder.append(":s").append(pageSize != null ? pageSize : SystemConstants.DEFAULT_PAGE_SIZE);

        // 其他参数生成哈希值
        if (params != null && !params.isEmpty()) {
            String hash = generateParamsHash(params);
            keyBuilder.append(":h:").append(hash);
        }

        return keyBuilder.toString();
    }

    /**
     * 增加 Sorted Set 中成员的分数
     *
     * @param key 键
     * @param member 成员
     * @param score 增加的分数
     * @return 增加后的分数
     */
    public Double zIncrementScore(String key, String member, double score) {
        try {
            return redisTemplate.opsForZSet().incrementScore(key, member, score);
        } catch (Exception e) {
            log.error("⚠️ Redis ZINCRBY失败: key={}, member={}, error={}", key, member, e.getMessage(), e);
            throw new RuntimeException("Redis操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取 Sorted Set 中指定范围的成员（按分数降序）
     *
     * @param key 键
     * @param start 开始位置
     * @param end 结束位置
     * @return 成员和分数的集合
     */
    public Set<ZSetOperations.TypedTuple<Object>> zReverseRangeWithScores(String key, long start, long end) {
        try {
            return redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
        } catch (Exception e) {
            log.error("⚠️ Redis ZREVRANGE失败: key={}, error={}", key, e.getMessage(), e);
            throw new RuntimeException("Redis操作失败: " + e.getMessage(), e);
        }
    }

    /**
     * 生成参数哈希值（MD5前8位）
     * 
     * @param params 参数Map
     * @return 8位十六进制哈希值
     */
    private String generateParamsHash(Map<String, Object> params) {
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
            log.error("生成参数哈希失败", e);
            // 降级方案：使用简单的hashCode
            return String.valueOf(params.hashCode()).replace("-", "n");
        }
    }
}

