package org.example.ais_sst.service.userService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.password.PasswordResetConfirmDTO;
import org.example.ais_sst.dto.password.PasswordResetRequestDTO;
import org.example.ais_sst.dto.password.PasswordResetVerifyDTO;
import org.example.ais_sst.entity.User;
import org.example.ais_sst.exception.PasswordResetException;
import org.example.ais_sst.exception.UserDoesNotExistException;
import org.example.ais_sst.repository.UserRepository;
import org.example.ais_sst.service.notificationService.EmailService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    private static final String RESET_CODE_PREFIX = "password_reset:";
    private static final String RESET_ATTEMPTS_PREFIX = "password_reset_attempts:";
    private static final int CODE_EXPIRATION_HOURS = 24;
    private static final int MAX_ATTEMPTS = 5;
    private static final int ATTEMPTS_TTL_MINUTES = 60;
    private static final SecureRandom secureRandom = new SecureRandom();

    /**
     * Запрос на сброс пароля - отправка кода на почту
     */
    public void requestPasswordReset(PasswordResetRequestDTO request) {
        String email = request.getEmail().trim().toLowerCase();

        // Проверяем, существует ли пользователь
        User user = userRepository.findByStudentEmail(email)
                .orElseThrow(() -> new UserDoesNotExistException("Пользователь с таким email не найден"));

        // Проверяем лимит попыток
        String attemptsKey = RESET_ATTEMPTS_PREFIX + email;
        Integer attempts = (Integer) redisTemplate.opsForValue().get(attemptsKey);

        if (attempts != null && attempts >= MAX_ATTEMPTS) {
            Long ttl = redisTemplate.getExpire(attemptsKey, TimeUnit.MINUTES);
            throw new PasswordResetException("Слишком много попыток. Попробуйте через " + ttl + " минут.");
        }

        // Генерируем 6-значный код
        String code = generateResetCode();

        // Сохраняем код в Redis (на 24 часа)
        String codeKey = RESET_CODE_PREFIX + email;
        redisTemplate.opsForValue().set(codeKey, code, CODE_EXPIRATION_HOURS, TimeUnit.HOURS);

        // Увеличиваем счетчик попыток
        if (attempts == null) {
            redisTemplate.opsForValue().set(attemptsKey, 1, ATTEMPTS_TTL_MINUTES, TimeUnit.MINUTES);
        } else {
            // ✅ ИСПРАВЛЕНО: используем opsForValue().increment()
            redisTemplate.opsForValue().increment(attemptsKey);
        }

        // Отправляем код на почту
        try {
            emailService.sendPasswordResetCode(email, code, user.getName());
            log.info("Password reset code sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send password reset email to: {}", email, e);
            // Удаляем код и декрементим счетчик при ошибке отправки
            redisTemplate.delete(codeKey);
            // ✅ ИСПРАВЛЕНО: используем opsForValue().decrement()
            Long remaining = redisTemplate.opsForValue().decrement(attemptsKey);
            if (remaining != null && remaining <= 0) {
                redisTemplate.delete(attemptsKey);
            }
            throw new PasswordResetException("Не удалось отправить код на почту. Попробуйте позже.");
        }
    }

    /**
     * Проверка кода и смена пароля
     */
    @Transactional
    public void verifyCodeAndResetPassword(PasswordResetVerifyDTO request) {
        String email = request.getEmail().trim().toLowerCase();
        String code = request.getCode().trim();
        String newPassword = request.getNewPassword();

        // Проверяем код в Redis
        String codeKey = RESET_CODE_PREFIX + email;
        String storedCode = (String) redisTemplate.opsForValue().get(codeKey);

        if (storedCode == null) {
            throw new PasswordResetException("Код истек или не найден. Запросите новый код.");
        }

        if (!storedCode.equals(code)) {
            throw new PasswordResetException("Неверный код подтверждения.");
        }

        // Находим пользователя
        User user = userRepository.findByStudentEmail(email)
                .orElseThrow(() -> new UserDoesNotExistException("Пользователь не найден"));

        // Обновляем пароль
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Удаляем код из Redis
        redisTemplate.delete(codeKey);
        redisTemplate.delete(RESET_ATTEMPTS_PREFIX + email);

        log.info("Password successfully reset for user: {}", email);
    }

    /**
     * Проверка кода (без смены пароля)
     */
    public boolean verifyCode(PasswordResetConfirmDTO request) {
        String email = request.getEmail().trim().toLowerCase();
        String code = request.getCode().trim();

        String codeKey = RESET_CODE_PREFIX + email;
        String storedCode = (String) redisTemplate.opsForValue().get(codeKey);

        if (storedCode == null) {
            throw new PasswordResetException("Код истек или не найден. Запросите новый код.");
        }

        return storedCode.equals(code);
    }

    /**
     * Генерация 6-значного кода
     */
    private String generateResetCode() {
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }
}