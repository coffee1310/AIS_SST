package org.example.ais_sst.service.tokens;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Slf4j
@Service
@RequiredArgsConstructor
public class RevokedTokenService {

    // Храним отозванные access токены (по JTI)
    private final Map<String, Long> revokedAccessTokens = new ConcurrentHashMap<>();

    // Храним отозванные refresh токены
    private final Map<String, Long> revokedRefreshTokens = new ConcurrentHashMap<>();

    // Храним все JTI токенов пользователя
    private final Map<Long, Set<String>> userActiveTokens = new ConcurrentHashMap<>();

    /**
     * Сохранить активный токен пользователя (по JTI)
     */
    public void storeActiveToken(Long userId, String jti, long expirationTime) {
        Set<String> userTokens = userActiveTokens.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>());
        userTokens.add(jti);
        log.info("Active token stored for user: {}, JTI: {}, total tokens: {}", userId, jti, userTokens.size());
    }

    /**
     * Отозвать access токен по JTI
     */
    public void revokeAccessToken(String jti) {
        if (jti != null && !jti.isEmpty()) {
            revokedAccessTokens.put(jti, System.currentTimeMillis());
            log.info("Access token revoked, JTI: {}", jti);
        }
    }

    /**
     * Отозвать refresh токен
     */
    public void revokeRefreshToken(String token) {
        if (token != null && !token.isEmpty()) {
            revokedRefreshTokens.put(token, System.currentTimeMillis());
            log.info("Refresh token revoked: {}", token);
        }
    }

    /**
     * Проверить, отозван ли access токен по JTI
     */
    public boolean isAccessTokenRevoked(String jti) {
        if (jti == null) return false;

        Boolean isRevoked = revokedAccessTokens.containsKey(jti);
        if (isRevoked) {
            log.debug("Access token is revoked, JTI: {}", jti);
        }
        return isRevoked;
    }

    /**
     * Проверить, отозван ли refresh токен
     */
    public boolean isRefreshTokenRevoked(String token) {
        if (token == null) return false;

        Boolean isRevoked = revokedRefreshTokens.containsKey(token);
        if (isRevoked) {
            log.debug("Refresh token is revoked: {}", token);
        }
        return isRevoked;
    }

    /**
     * Отозвать ВСЕ токены пользователя
     */
    public void revokeAllUserTokens(Long userId) {
        Set<String> userTokens = userActiveTokens.remove(userId);
        if (userTokens != null && !userTokens.isEmpty()) {
            for (String jti : userTokens) {
                revokedAccessTokens.put(jti, System.currentTimeMillis());
                log.info("Token revoked for user: {}, JTI: {}", userId, jti);
            }
            log.info("All {} tokens revoked for user: {}", userTokens.size(), userId);
        } else {
            log.info("No active tokens found for user: {}", userId);
        }
    }

    /**
     * Очистить просроченные токены (опционально)
     */
    public void cleanupExpiredTokens() {
        // Можно добавить логику очистки, если нужно
    }
}