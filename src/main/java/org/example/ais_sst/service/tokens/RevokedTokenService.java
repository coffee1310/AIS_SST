package org.example.ais_sst.service.tokens;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.service.redisService.RedisKeys;
import org.example.ais_sst.service.redisService.RedisService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RevokedTokenService {

    private final RedisService redisService;

    @Value("${app.jwtExpirationMs:900000}")
    private long accessTokenTtlSeconds;

    /**
     * Отозвать access токен по JTI
     */
    public void revokeAccessToken(String jti) {
        if (jti == null || jti.isEmpty()) {
            log.warn("Cannot revoke access token: JTI is null or empty");
            return;
        }

        String key = RedisKeys.revokedAccessToken(jti);
        long ttlSeconds = accessTokenTtlSeconds / 1000;
        redisService.set(key, "revoked", ttlSeconds);
        log.info("Access token revoked, JTI: {}, TTL: {} seconds", jti, ttlSeconds);
    }

    /**
     * Проверить, отозван ли access токен
     */
    public boolean isAccessTokenRevoked(String jti) {
        if (jti == null || jti.isEmpty()) {
            return false;
        }

        String key = RedisKeys.revokedAccessToken(jti);
        boolean revoked = redisService.exists(key);
        if (revoked) {
            log.debug("Access token is revoked, JTI: {}", jti);
        }
        return revoked;
    }

    /**
     * Отозвать refresh токен
     */
    public void revokeRefreshToken(String token) {
        if (token == null || token.isEmpty()) {
            log.warn("Cannot revoke refresh token: token is null or empty");
            return;
        }

        String key = RedisKeys.revokedRefreshToken(token);
        redisService.set(key, "revoked", 86400); // 24 часа
        log.info("Refresh token revoked: {}", token);
    }

    /**
     * Проверить, отозван ли refresh токен
     */
    public boolean isRefreshTokenRevoked(String token) {
        if (token == null || token.isEmpty()) {
            return false;
        }

        String key = RedisKeys.revokedRefreshToken(token);
        boolean revoked = redisService.exists(key);
        if (revoked) {
            log.debug("Refresh token is revoked: {}", token);
        }
        return revoked;
    }

    /**
     * Сохранить активный access токен пользователя
     */
    public void storeActiveToken(Long userId, String jti, long expirationTime) {
        if (userId == null || jti == null || jti.isEmpty()) {
            log.warn("Cannot store active token: userId or JTI is null");
            return;
        }

        String userTokensKey = RedisKeys.userActiveTokens(userId);
        String userCurrentKey = RedisKeys.userCurrentToken(userId);

        redisService.addToSet(userTokensKey, jti);
        long ttlSeconds = (expirationTime - System.currentTimeMillis()) / 1000;
        if (ttlSeconds > 0) {
            redisService.expire(userTokensKey, ttlSeconds);
        }
        redisService.set(userCurrentKey, jti, ttlSeconds);

        log.info("Active token stored for user: {}, JTI: {}, TTL: {} seconds", userId, jti, ttlSeconds);
    }

    /**
     * Отозвать ВСЕ токены пользователя
     */
    public void revokeAllUserTokens(Long userId) {
        if (userId == null) {
            log.warn("Cannot revoke all tokens: userId is null");
            return;
        }

        String userTokensKey = RedisKeys.userActiveTokens(userId);
        String userCurrentKey = RedisKeys.userCurrentToken(userId);

        Set<String> tokens = redisService.getSet(userTokensKey);

        if (tokens != null && !tokens.isEmpty()) {
            for (String jti : tokens) {
                revokeAccessToken(jti);
                log.debug("Token revoked for user: {}, JTI: {}", userId, jti);
            }
            log.info("All {} tokens revoked for user: {}", tokens.size(), userId);
        } else {
            log.info("No active tokens found for user: {}", userId);
        }

        redisService.delete(userTokensKey);
        redisService.delete(userCurrentKey);
    }

    /**
     * Получить текущий активный токен пользователя
     */
    public String getCurrentActiveToken(Long userId) {
        if (userId == null) {
            return null;
        }

        String userCurrentKey = RedisKeys.userCurrentToken(userId);
        Object value = redisService.get(userCurrentKey);

        if (value instanceof String) {
            return (String) value;
        }

        return value != null ? value.toString() : null;
    }
}