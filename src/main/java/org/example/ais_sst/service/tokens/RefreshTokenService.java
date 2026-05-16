package org.example.ais_sst.service.tokens;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.entity.RefreshToken;
import org.example.ais_sst.entity.User;
import org.example.ais_sst.exception.TokenRefreshException;
import org.example.ais_sst.repository.RefreshTokenRepository;
import org.example.ais_sst.repository.UserRepository;
import org.example.ais_sst.service.redisService.RedisService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    @Value("${app.jwtRefreshExpirationMs}")
    private Long refreshTokenDurationMs;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final RedisService redisService;
    private final RevokedTokenService revokedTokenService;

    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    @Transactional
    public RefreshToken createRefreshToken(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        // Удаляем ВСЕ старые refresh токены пользователя из БД
        refreshTokenRepository.deleteByUserId(userId);
        log.info("Deleted all old refresh tokens for user: {}", userId);

        RefreshToken refreshToken = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenDurationMs))
                .build();

        RefreshToken savedToken = refreshTokenRepository.save(refreshToken);
        log.info("Created new refresh token for user: {}", userId);

        return savedToken;
    }

    public RefreshToken verifyExpiration(RefreshToken token) {
        if (token.getExpiryDate().compareTo(Instant.now()) < 0) {
            refreshTokenRepository.delete(token);
            // Также добавляем в Redis blacklist
            revokedTokenService.revokeRefreshToken(token.getToken());
            log.warn("Refresh token expired: {}", token.getToken());
            throw new TokenRefreshException(token.getToken(), "Refresh token expired. Please login again.");
        }
        return token;
    }

    @Transactional
    public void revokeRefreshToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshToken -> {
            refreshTokenRepository.delete(refreshToken);
            // Добавляем в Redis blacklist
            revokedTokenService.revokeRefreshToken(token);
            log.info("Revoked refresh token: {}", token);
        });
    }

    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
        log.info("Revoked all refresh tokens for user: {}", userId);
    }
}