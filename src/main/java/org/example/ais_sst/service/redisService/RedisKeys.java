package org.example.ais_sst.service.redisService;

public class RedisKeys {

    // Префиксы для различных типов данных
    private static final String PREFIX_TOKEN = "token:";
    private static final String PREFIX_USER = "user:";
    private static final String PREFIX_CACHE = "cache:";
    private static final String PREFIX_RATE_LIMIT = "ratelimit:";
    private static final String PREFIX_SESSION = "session:";
    private static final String PREFIX_STATS = "stats:";

    // Ключи для токенов
    public static String revokedAccessToken(String jti) {
        return PREFIX_TOKEN + "revoked:access:" + jti;
    }

    public static String revokedRefreshToken(String token) {
        return PREFIX_TOKEN + "revoked:refresh:" + token;
    }

    public static String userActiveTokens(Long userId) {
        return PREFIX_USER + userId + ":tokens:active";
    }

    public static String userCurrentToken(Long userId) {
        return PREFIX_USER + userId + ":token:current";
    }

    // Ключи для кэширования
    public static String cache(String entity, Long id) {
        return PREFIX_CACHE + entity + ":" + id;
    }

    public static String cache(String entity, String key) {
        return PREFIX_CACHE + entity + ":" + key;
    }

    // Ключи для rate limiting
    public static String rateLimit(String endpoint, String userId) {
        return PREFIX_RATE_LIMIT + endpoint + ":" + userId;
    }

    // Ключи для сессий
    public static String session(String sessionId) {
        return PREFIX_SESSION + sessionId;
    }

    public static String userSessions(Long userId) {
        return PREFIX_USER + userId + ":sessions";
    }

    public static String cacheStats(String name) {
        return PREFIX_STATS + "cache:" + name;
    }
}