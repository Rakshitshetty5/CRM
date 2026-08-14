package com.flowcrm.common.lock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisDistributedLockService {

    private static final String UNLOCK_LUA = """
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            else
                return 0
            end
            """;

    private final StringRedisTemplate redisTemplate;
    private final RedisScript<Long> unlockScript = RedisScript.of(UNLOCK_LUA, Long.class);

    /**
     * Tries to acquire a distributed lock using Redis SET key value NX EX.
     *
     * @param lockKey   the lock key name
     * @param lockValue unique identifier for the lock attempt (e.g. UUID)
     * @param ttl       lock expiration time
     * @return true if lock was acquired, false otherwise
     */
    public boolean tryLock(String lockKey, String lockValue, Duration ttl) {
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, ttl);
            return Boolean.TRUE.equals(acquired);
        } catch (Exception e) {
            log.warn("Failed to acquire Redis lock for key={}: {}", lockKey, e.getMessage());
            return false;
        }
    }

    /**
     * Releases the distributed lock atomically via Lua script if and only if
     * the current lock value matches the provided lockValue.
     *
     * @param lockKey   the lock key name
     * @param lockValue unique identifier for the lock attempt (e.g. UUID)
     * @return true if lock was successfully released, false otherwise
     */
    public boolean unlock(String lockKey, String lockValue) {
        try {
            Long result = redisTemplate.execute(unlockScript, List.of(lockKey), lockValue);
            return result != null && result > 0;
        } catch (Exception e) {
            log.warn("Failed to release Redis lock for key={}: {}", lockKey, e.getMessage());
            return false;
        }
    }
}
