package com.hospital.service.impl;

import com.hospital.service.RateLimitService;
import com.hospital.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 基于Redis的限流实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitServiceImpl implements RateLimitService {

    private static final String RATE_LIMIT_PREFIX = "hospital:ratelimit:";

    private final RedisUtil redisUtil;

    @Override
    public boolean tryAcquire(String key, int limit, long windowSeconds) {
        if (limit <= 0 || windowSeconds <= 0) {
            return true;
        }

        String redisKey = RATE_LIMIT_PREFIX + key;
        Long count = redisUtil.increment(redisKey, 1);
        if (count != null && count == 1) {
            redisUtil.expire(redisKey, windowSeconds, TimeUnit.SECONDS);
        }
        boolean allowed = count != null && count <= limit;
        if (!allowed) {
            log.warn("RateLimit triggered: key={}, count={}, limit={}, window={}s", key, count, limit, windowSeconds);
        }
        return allowed;
    }
}


