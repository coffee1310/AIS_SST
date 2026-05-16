package org.example.ais_sst.service.redisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    public void set(String key, Object value, long ttlSeconds) {
        try {
            redisTemplate.opsForValue().set(key, value, ttlSeconds, TimeUnit.SECONDS);
            log.debug("Redis SET: {} (TTL: {}s)", key, ttlSeconds);
        } catch (Exception e) {
            log.error("Failed to set Redis key: {}", key, e);
        }
    }

    public void set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            log.debug("Redis SET: {}", key);
        } catch (Exception e) {
            log.error("Failed to set Redis key: {}", key, e);
        }
    }

    public Object get(String key) {
        try {
            Object value = redisTemplate.opsForValue().get(key);
            log.debug("Redis GET: {} = {}", key, value);
            return value;
        } catch (Exception e) {
            log.error("Failed to get Redis key: {}", key, e);
            return null;
        }
    }

    public void delete(String key) {
        try {
            redisTemplate.delete(key);
            log.debug("Redis DELETE: {}", key);
        } catch (Exception e) {
            log.error("Failed to delete Redis key: {}", key, e);
        }
    }

    public boolean exists(String key) {
        try {
            Boolean exists = redisTemplate.hasKey(key);
            log.debug("Redis EXISTS: {} = {}", key, exists);
            return Boolean.TRUE.equals(exists);
        } catch (Exception e) {
            log.error("Failed to check Redis key: {}", key, e);
            return false;
        }
    }

    public void expire(String key, long ttlSeconds) {
        try {
            redisTemplate.expire(key, ttlSeconds, TimeUnit.SECONDS);
            log.debug("Redis EXPIRE: {} (TTL: {}s)", key, ttlSeconds);
        } catch (Exception e) {
            log.error("Failed to set expire for key: {}", key, e);
        }
    }

    public void addToSet(String key, String value) {
        try {
            redisTemplate.opsForSet().add(key, value);
            log.debug("Redis SADD: {} -> {}", key, value);
        } catch (Exception e) {
            log.error("Failed to add to Redis set: {}", key, e);
        }
    }

    public void removeFromSet(String key, String value) {
        try {
            redisTemplate.opsForSet().remove(key, value);
            log.debug("Redis SREM: {} -> {}", key, value);
        } catch (Exception e) {
            log.error("Failed to remove from Redis set: {}", key, e);
        }
    }

    @SuppressWarnings("unchecked")
    public Set<String> getSet(String key) {
        try {
            Set<String> members = (Set<String>) (Object) redisTemplate.opsForSet().members(key);
            log.debug("Redis SMEMBERS: {} (size: {})", key, members != null ? members.size() : 0);
            return members;
        } catch (Exception e) {
            log.error("Failed to get Redis set: {}", key, e);
            return Set.of();
        }
    }

    public long incrementWithExpire(String key, long ttlSeconds) {
        try {
            Long value = redisTemplate.opsForValue().increment(key);
            if (value != null && value == 1) {
                expire(key, ttlSeconds);
            }
            log.debug("Redis INCR: {} = {}", key, value);
            return value != null ? value : 0;
        } catch (Exception e) {
            log.error("Failed to increment Redis key: {}", key, e);
            return 0;
        }
    }

    public Set<String> keys(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            log.debug("Redis KEYS: {} (found: {})", pattern, keys != null ? keys.size() : 0);
            return keys;
        } catch (Exception e) {
            log.error("Failed to get Redis keys by pattern: {}", pattern, e);
            return Set.of();
        }
    }

    public void deleteByPattern(String pattern) {
        try {
            Set<String> keys = keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("Redis DELETED {} keys by pattern: {}", keys.size(), pattern);
            }
        } catch (Exception e) {
            log.error("Failed to delete Redis keys by pattern: {}", pattern, e);
        }
    }
}
