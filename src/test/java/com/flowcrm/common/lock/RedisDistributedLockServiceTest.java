package com.flowcrm.common.lock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisDistributedLockServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisDistributedLockService lockService;

    @BeforeEach
    void setUp() {
        lockService = new RedisDistributedLockService(redisTemplate);
    }

    @Test
    @DisplayName("tryLock returns true when setIfAbsent succeeds")
    void tryLock_Success() {
        String key = "lock:follow-up-reminder";
        String value = UUID.randomUUID().toString();
        Duration ttl = Duration.ofSeconds(120);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(key, value, ttl)).thenReturn(true);

        boolean result = lockService.tryLock(key, value, ttl);

        assertTrue(result);
        verify(valueOperations).setIfAbsent(key, value, ttl);
    }

    @Test
    @DisplayName("tryLock returns false when setIfAbsent returns false or null")
    void tryLock_Failure() {
        String key = "lock:follow-up-reminder";
        String value = UUID.randomUUID().toString();
        Duration ttl = Duration.ofSeconds(120);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(key, value, ttl)).thenReturn(false);

        boolean result = lockService.tryLock(key, value, ttl);

        assertFalse(result);
    }

    @Test
    @DisplayName("tryLock returns false on Redis exception")
    void tryLock_Exception() {
        String key = "lock:follow-up-reminder";
        String value = UUID.randomUUID().toString();
        Duration ttl = Duration.ofSeconds(120);

        when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Redis connection refused"));

        boolean result = lockService.tryLock(key, value, ttl);

        assertFalse(result);
    }

    @Test
    @DisplayName("unlock executes Lua script and returns true on success")
    void unlock_Success() {
        String key = "lock:follow-up-reminder";
        String value = UUID.randomUUID().toString();

        when(redisTemplate.execute(any(RedisScript.class), eq(List.of(key)), eq(value))).thenReturn(1L);

        boolean result = lockService.unlock(key, value);

        assertTrue(result);
    }

    @Test
    @DisplayName("unlock returns false when Lua script returns 0 (lock value mismatch or key expired)")
    void unlock_MismatchOrExpired() {
        String key = "lock:follow-up-reminder";
        String value = UUID.randomUUID().toString();

        when(redisTemplate.execute(any(RedisScript.class), eq(List.of(key)), eq(value))).thenReturn(0L);

        boolean result = lockService.unlock(key, value);

        assertFalse(result);
    }
}
