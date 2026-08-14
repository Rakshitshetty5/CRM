package com.flowcrm.common.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SlidingWindowLuaLimiterTest {

    @Mock
    private StringRedisTemplate redis;

    private SlidingWindowLuaLimiter limiter;

    @BeforeEach
    void setUp() {
        limiter = new SlidingWindowLuaLimiter(redis);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCheckAllowed() {
        when(redis.execute(any(RedisScript.class), anyList(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(1L, 9L, 0L));

        RateLimiterResult result = limiter.check("testKey", 10, 60L);

        assertTrue(result.isAllowed());
        assertEquals(9, result.remaining());
        assertEquals(0, result.retryAfterSeconds());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCheckDenied() {
        long nowMs = System.currentTimeMillis();
        long oldestScoreMs = nowMs - 30_000; // 30 seconds ago

        when(redis.execute(any(RedisScript.class), anyList(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(0L, 0L, oldestScoreMs));

        RateLimiterResult result = limiter.check("testKey", 10, 60L);

        assertFalse(result.isAllowed());
        assertEquals(0, result.remaining());
        assertTrue(result.retryAfterSeconds() > 0 && result.retryAfterSeconds() <= 60);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCheckFailsOpenOnDataAccessException() {
        when(redis.execute(any(RedisScript.class), anyList(), any(), any(), any(), any(), any()))
                .thenThrow(new QueryTimeoutException("Redis timeout"));

        RateLimiterResult result = limiter.check("testKey", 10, 60L);

        assertTrue(result.isAllowed());
        assertEquals(10, result.remaining());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCheckFailsOpenOnNullResult() {
        when(redis.execute(any(RedisScript.class), anyList(), any(), any(), any(), any(), any()))
                .thenReturn(null);

        RateLimiterResult result = limiter.check("testKey", 10, 60L);

        assertTrue(result.isAllowed());
        assertEquals(10, result.remaining());
    }
}
