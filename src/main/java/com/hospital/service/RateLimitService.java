package com.hospital.service;

/**
 * 限流服务
 */
public interface RateLimitService {

    /**
     * 尝试获取访问令牌
     *
     * @param key           限流键
     * @param limit         时间窗口内允许的最大次数
     * @param windowSeconds 时间窗口（秒）
     * @return true 表示允许访问，false 表示被限流
     */
    boolean tryAcquire(String key, int limit, long windowSeconds);
}


