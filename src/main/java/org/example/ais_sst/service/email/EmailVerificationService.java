package org.example.ais_sst.service.email;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestsSummaryDTO;
import org.example.ais_sst.dto.account_request.EmailVerificationDTO;
import org.example.ais_sst.entity.AccountCreatingRequest;
import org.example.ais_sst.entity.Group;
import org.example.ais_sst.entity.Speciality;
import org.example.ais_sst.entity.enums.AccountCreatingRequestStatus;
import org.example.ais_sst.exception.EmailAlreadyExistsException;
import org.example.ais_sst.exception.GroupDoesNotExistException;
import org.example.ais_sst.exception.SpecialityDoesNotExistException;
import org.example.ais_sst.exception.VerificationCodeException;
import org.example.ais_sst.mapper.AccountCreatingRequestMapper;
import org.example.ais_sst.repository.AccountCreatingRequestsRepository;
import org.example.ais_sst.repository.GroupRepository;
import org.example.ais_sst.repository.SpecialityRepository;
import org.example.ais_sst.service.accountCreatingRequestsService.AccountRequestPhotoService;
import org.example.ais_sst.service.notificationService.EmailService;
import org.example.ais_sst.service.socialStatusService.AccountCreatingRequestsSocialStatusService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final AccountCreatingRequestsRepository accountCreatingRequestsRepository;
    private final EmailService emailService;

    private static final String VERIFICATION_CODE_PREFIX = "email_verification:";
    private static final String VERIFICATION_ATTEMPTS_PREFIX = "email_verification_attempts:";
    private static final int CODE_EXPIRATION_MINUTES = 15;
    private static final int MAX_ATTEMPTS = 5;
    private static final int ATTEMPTS_TTL_MINUTES = 60;
    private static final SecureRandom secureRandom = new SecureRandom();
    ;
    private final GroupRepository groupRepository;
    private final SpecialityRepository specialityRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountCreatingRequestMapper requestMapper;
    private final AccountRequestPhotoService accountRequestPhotoService;
    private final AccountCreatingRequestsSocialStatusService accountCreatingRequestsSocialStatusService;

    /**
     * Отправка кода верификации на email
     */
    public void sendVerificationCode(String email, String name) {
        email = email.trim().toLowerCase();

        // Проверяем, не зарегистрирован ли уже этот email
        if (accountCreatingRequestsRepository.existsByStudentEmail(email)) {
            throw new EmailAlreadyExistsException("Заявка с этим email уже существует");
        }

        // Проверяем лимит попыток
        String attemptsKey = VERIFICATION_ATTEMPTS_PREFIX + email;
        Integer attempts = (Integer) redisTemplate.opsForValue().get(attemptsKey);

        if (attempts != null && attempts >= MAX_ATTEMPTS) {
            Long ttl = redisTemplate.getExpire(attemptsKey, TimeUnit.MINUTES);
            throw new VerificationCodeException("Слишком много попыток. Попробуйте через " + ttl + " минут.");
        }

        // Генерируем код
        String code = generateVerificationCode();

        // Сохраняем в Redis
        String codeKey = VERIFICATION_CODE_PREFIX + email;
        redisTemplate.opsForValue().set(codeKey, code, CODE_EXPIRATION_MINUTES, TimeUnit.MINUTES);

        // Увеличиваем счетчик попыток
        if (attempts == null) {
            redisTemplate.opsForValue().set(attemptsKey, 1, ATTEMPTS_TTL_MINUTES, TimeUnit.MINUTES);
        } else {
            redisTemplate.opsForValue().increment(attemptsKey);
        }

        // Отправляем email
        try {
            emailService.sendEmailVerificationCode(email, code, name);
            log.info("Verification code sent to: {}", email);
        } catch (Exception e) {
            log.error("Failed to send verification email to: {}", email, e);
            // Откатываем изменения в Redis
            redisTemplate.delete(codeKey);
            Long remaining = redisTemplate.opsForValue().decrement(attemptsKey);
            if (remaining != null && remaining <= 0) {
                redisTemplate.delete(attemptsKey);
            }
            throw new VerificationCodeException("Не удалось отправить код на почту. Попробуйте позже.");
        }
    }

    /**
     * Подтверждение email и создание заявки
     */
    @Transactional
    public AccountCreatingRequest verifyEmailAndCreateRequest(
            String email,
            String code,
            AccountCreatingRequestsSummaryDTO dto) {

        email = email.trim().toLowerCase();

        // Проверяем код
        String codeKey = VERIFICATION_CODE_PREFIX + email;
        String storedCode = (String) redisTemplate.opsForValue().get(codeKey);

        if (storedCode == null) {
            throw new VerificationCodeException("Код истек или не найден. Запросите новый код.");
        }

        if (!storedCode.equals(code)) {
            throw new VerificationCodeException("Неверный код подтверждения.");
        }

        // Проверяем, не создана ли уже заявка
        if (accountCreatingRequestsRepository.existsByStudentEmail(email)) {
            throw new EmailAlreadyExistsException("Заявка с этим email уже существует");
        }

        // Создаем заявку со статусом "НА_РАССМОТРЕНИИ"
        AccountCreatingRequest request = createAccountRequestInternal(dto);
        request.setStatus(AccountCreatingRequestStatus.НА_РАССМОТРЕНИИ);
        AccountCreatingRequest savedRequest = accountCreatingRequestsRepository.save(request);

        // Удаляем код из Redis
        redisTemplate.delete(codeKey);
        redisTemplate.delete(VERIFICATION_ATTEMPTS_PREFIX + email);

        log.info("Account request created after email verification: {}", email);
        return savedRequest;
    }

    /**
     * Повторная отправка кода
     */
    public void resendVerificationCode(String email) {
        email = email.trim().toLowerCase();

        // Проверяем, не создана ли уже заявка
        if (accountCreatingRequestsRepository.existsByStudentEmail(email)) {
            throw new EmailAlreadyExistsException("Заявка с этим email уже существует");
        }

        // Удаляем старый код
        String codeKey = VERIFICATION_CODE_PREFIX + email;
        redisTemplate.delete(codeKey);

        // Отправляем новый код (без имени, т.к. мы его не знаем)
        sendVerificationCode(email, "пользователь");
    }

    /**
     * Проверка кода (без создания заявки)
     */
    public boolean verifyCode(String email, String code) {
        email = email.trim().toLowerCase();
        String codeKey = VERIFICATION_CODE_PREFIX + email;
        String storedCode = (String) redisTemplate.opsForValue().get(codeKey);

        if (storedCode == null) {
            throw new VerificationCodeException("Код истек или не найден. Запросите новый код.");
        }

        return storedCode.equals(code);
    }

    private String generateVerificationCode() {
        int code = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(code);
    }

    @Transactional
    public AccountCreatingRequest createAccountRequestInternal(AccountCreatingRequestsSummaryDTO dto) {
        // Проверяем уникальность email
        if (accountCreatingRequestsRepository.existsByStudentEmail(dto.getStudentEmail())) {
            throw new EmailAlreadyExistsException("Заявка с этим email уже существует");
        }

        // Находим группу
        Group userGroup = groupRepository.findGroupById(dto.getGroup_id())
                .orElseThrow(() -> new GroupDoesNotExistException("Группа не найдена"));

        // Находим специальность
        Speciality userSpeciality = specialityRepository.findSpecialityById(dto.getSpeciality_id())
                .orElseThrow(() -> new SpecialityDoesNotExistException("Специальность не найдена"));

        // Создаем заявку
        AccountCreatingRequest request = requestMapper.toEntity(dto);
        request.setGroup(userGroup);
        request.setSpeciality(userSpeciality);
        request.setPassword(passwordEncoder.encode(dto.getPassword()));
        request.setStatus(AccountCreatingRequestStatus.НА_РАССМОТРЕНИИ);

        log.info("Account request internal created for email: {}", dto.getStudentEmail());
        return request;
    }
}