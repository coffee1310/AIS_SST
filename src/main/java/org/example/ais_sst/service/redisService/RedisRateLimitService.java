package org.example.ais_sst.service.redisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisRateLimitService {

    private final RedisService redisService;

    /**
     * Проверить лимит запросов для endpoint
     * @param key уникальный ключ (например, endpoint + userId)
     * @param maxRequests максимальное количество запросов за период
     * @param windowSeconds период в секундах
     * @return true - если лимит превышен, false - если можно выполнить запрос
     */
    public boolean isRateLimited(String key, int maxRequests, long windowSeconds) {
        long currentCount = redisService.incrementWithExpire(key, windowSeconds);

        boolean isLimited = currentCount > maxRequests;

        if (isLimited) {
            log.warn("Rate limit exceeded for key: {}, count: {}/{}", key, currentCount, maxRequests);
        } else {
            log.debug("Rate limit OK for key: {}, count: {}/{}", key, currentCount, maxRequests);
        }

        return isLimited;
    }

    /**
     * Сбросить лимит для ключа
     */
    public void resetRateLimit(String key) {
        redisService.delete(key);
        log.debug("Rate limit reset for key: {}", key);
    }
}