package com.hospital.service;

import com.hospital.service.impl.RateLimitServiceImpl;
import com.hospital.util.RedisUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceImplTest {

    @Mock
    private RedisUtil redisUtil;

    @InjectMocks
    private RateLimitServiceImpl rateLimitService;

    @BeforeEach
    void setUp() {
        rateLimitService = new RateLimitServiceImpl(redisUtil);
    }

    @Test
    void tryAcquire_shouldAllowFirstRequestAndSetExpire() {
        String key = "login";
        when(redisUtil.increment("hospital:ratelimit:" + key, 1)).thenReturn(1L);
        when(redisUtil.expire(eq("hospital:ratelimit:" + key), eq(60L), eq(TimeUnit.SECONDS))).thenReturn(true);

        boolean allowed = rateLimitService.tryAcquire(key, 5, 60);

        assertThat(allowed).isTrue();
        verify(redisUtil).expire("hospital:ratelimit:" + key, 60L, TimeUnit.SECONDS);
    }

    @Test
    void tryAcquire_shouldRejectWhenExceedLimit() {
        String key = "login";
        when(redisUtil.increment("hospital:ratelimit:" + key, 1)).thenReturn(6L);

        boolean allowed = rateLimitService.tryAcquire(key, 5, 60);

        assertThat(allowed).isFalse();
        verify(redisUtil, never()).expire(anyString(), anyLong(), any());
    }
}


