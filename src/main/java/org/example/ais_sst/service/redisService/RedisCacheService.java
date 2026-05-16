package org.example.ais_sst.service.redisService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisCacheService {

    private final RedisService redisService;

    /**
     * Получить данные из кэша или загрузить через supplier
     */
    public <T> T getOrLoad(String key, Supplier<T> loader, long ttlSeconds, Class<T> type) {
        // Пытаемся получить из кэша
        String cachedValue = redisService.get(key);

        if (cachedValue != null) {
            log.debug("Cache HIT for key: {}", key);
            // Здесь нужна десериализация, для простоты используем строки
            return (T) cachedValue;
        }

        log.debug("Cache MISS for key: {}", key);

        // Загружаем данные
        T value = loader.get();

        if (value != null) {
            // Сохраняем в кэш
            redisService.set(key, value.toString(), ttlSeconds);
        }

        return value;
    }

    /**
     * Инвалидировать кэш по ключу
     */
    public void invalidate(String key) {
        redisService.delete(key);
        log.debug("Cache invalidated for key: {}", key);
    }

    /**
     * Инвалидировать кэш по паттерну
     */
    public void invalidateByPattern(String pattern) {
        redisService.deleteByPattern(pattern);
        log.debug("Cache invalidated by pattern: {}", pattern);
    }
}