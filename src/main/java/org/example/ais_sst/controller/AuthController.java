package org.example.ais_sst.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.example.ais_sst.controller.base.BaseController;
import org.example.ais_sst.dto.account_request.AccountCreatingRequestsSummaryDTO;
import org.example.ais_sst.dto.account_request.EmailVerificationDTO;
import org.example.ais_sst.dto.account_request.ResendVerificationCodeDTO;
import org.example.ais_sst.dto.password.PasswordResetConfirmDTO;
import org.example.ais_sst.dto.password.PasswordResetRequestDTO;
import org.example.ais_sst.dto.password.PasswordResetVerifyDTO;
import org.example.ais_sst.dto.request.LoginRequest;
import org.example.ais_sst.dto.request.RefreshTokenRequest;
import org.example.ais_sst.dto.response.JwtResponse;
import org.example.ais_sst.dto.user.UserSummaryDTO;
import org.example.ais_sst.entity.*;
import org.example.ais_sst.entity.enums.Gender;
import org.example.ais_sst.exception.GroupDoesNotExistException;
import org.example.ais_sst.exception.SpecialityDoesNotExistException;
import org.example.ais_sst.exception.TokenRefreshException;
import org.example.ais_sst.repository.*;
import org.example.ais_sst.security.jwt.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.service.email.EmailVerificationService;
import org.example.ais_sst.service.redisService.RedisKeys;
import org.example.ais_sst.service.redisService.RedisRateLimitService;
import org.example.ais_sst.service.tokens.RefreshTokenService;
import org.example.ais_sst.service.tokens.RevokedTokenService;
import org.example.ais_sst.service.userService.PasswordResetService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController extends BaseController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SpecialityRepository specialityRepository;
    private final GroupRepository groupRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RefreshTokenService refreshTokenService;
    private final SocialStatusStudentsRepository socialStatusStudentRepository;
    private final RevokedTokenService revokedTokenService;
    private final RedisRateLimitService rateLimitService;
    private final HttpServletRequest httpServletRequest;
    private final PasswordResetService passwordResetService;


    @Value("${app.jwtExpirationMs}")
    private int jwtExpirationMs;

    private ResponseEntity<Map<String, Object>> createErrorResponse(HttpStatus status, String error, String message, String path) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        body.put("path", path);
        return ResponseEntity.status(status).body(body);
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        String clientIp = httpServletRequest.getRemoteAddr();
        String rateLimitKey = RedisKeys.rateLimit("login", clientIp);

        // Rate limiting: 5 попыток за 60 секунд
        if (rateLimitService.isRateLimited(rateLimitKey, 5, 60)) {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            return createErrorResponse(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too Many Requests",
                    "Too many login attempts. Please try again later.",
                    "/api/auth/login"
            );
        }

        logInfo("/api/auth/login", "Login attempt for email: {}", loginRequest.getEmail());

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getId();

        // 1. Отзываем ВСЕ старые токены пользователя
        refreshTokenService.revokeAllUserTokens(userId);
        revokedTokenService.revokeAllUserTokens(userId);

        log.info("All old tokens revoked for user: {}", userId);

        // 2. Генерируем новый access token
        String newAccessToken = jwtUtils.generateJwtToken(authentication);

        // 3. Получаем JTI из нового токена
        String jti = jwtUtils.getJtiFromToken(newAccessToken);

        // 4. Сохраняем JTI как активный
        long accessTokenExpiration = System.currentTimeMillis() + jwtExpirationMs;
        revokedTokenService.storeActiveToken(userId, jti, accessTokenExpiration);

        // 5. Создаем новый refresh token
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(userId);

        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        logInfo("/api/auth/login", "User logged in successfully: {}, new JTI: {}", loginRequest.getEmail(), jti);

        JwtResponse jwtResponse = new JwtResponse(
                newAccessToken,
                newRefreshToken.getToken(),
                userId,
                userDetails.getUsername(),
                userDetails.getName(),
                userDetails.getSurname(),
                roles);

        return createSuccessResponse("Login successful", jwtResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        logInfo("/api/auth/refresh", "Refresh token request");

        String requestRefreshToken = request.getRefreshToken();

        // Проверяем, не отозван ли refresh токен в Redis
        if (revokedTokenService.isRefreshTokenRevoked(requestRefreshToken)) {
            log.warn("Refresh token has been revoked: {}", requestRefreshToken);
            return createErrorResponse(
                    HttpStatus.UNAUTHORIZED,
                    "Token Revoked",
                    "Refresh token has been revoked. Please login again.",
                    "/api/auth/refresh"
            );
        }

        return refreshTokenService.findByToken(requestRefreshToken)
            .map(refreshToken -> {
                try {
                    refreshTokenService.verifyExpiration(refreshToken);

                    Long userId = refreshToken.getUser().getId();
                    User user = refreshToken.getUser();

                    // Отзываем старые токены
                    refreshTokenService.revokeRefreshToken(requestRefreshToken);
                    revokedTokenService.revokeRefreshToken(requestRefreshToken);
                    revokedTokenService.revokeAllUserTokens(userId);

                    // Создаем новые токены
                    RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(userId);

                    Authentication authentication = new UsernamePasswordAuthenticationToken(
                            user.getStudentEmail(), user.getPassword());
                    String newAccessToken = jwtUtils.generateJwtToken(authentication);

                    String jti = jwtUtils.getJtiFromToken(newAccessToken);
                    revokedTokenService.storeActiveToken(userId, jti, System.currentTimeMillis() + jwtExpirationMs);

                    List<String> roles = user.getRole() != null
                            ? List.of(user.getRole().getTitle())
                            : List.of("USER");

                    JwtResponse jwtResponse = new JwtResponse(
                            newAccessToken,
                            newRefreshToken.getToken(),
                            userId,
                            user.getStudentEmail(),
                            user.getName(),
                            user.getSurname(),
                            roles);

                    log.info("Tokens refreshed successfully for user: {}", userId);
                    return createSuccessResponse("Token refreshed successfully", jwtResponse);

                } catch (TokenRefreshException e) {
                    log.error("Refresh token expired: {}", e.getMessage());
                    return createErrorResponse(
                            HttpStatus.UNAUTHORIZED,
                            "Token Expired",
                            e.getMessage(),
                            "/api/auth/refresh"
                    );
                }
            })
            .orElse(createErrorResponse(
                    HttpStatus.UNAUTHORIZED,
                    "Token Not Found",
                    "Refresh token not found. Please login again.",
                    "/api/auth/refresh"
            ));
    }



    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody UserSummaryDTO userSummaryDTO) {
        logInfo("/api/auth/register", "Registration attempt for email: {}", userSummaryDTO.getStudentEmail());

        // Check if email already exists
        if (userRepository.existsByStudentEmail(userSummaryDTO.getStudentEmail())) {
            logWarn("/api/auth/register", "Registration failed - email already exists: {}",
                    userSummaryDTO.getStudentEmail());
            return ResponseEntity.badRequest().body("Ошибка: Email уже используется!");
        }

        // Check if phone already exists
        if (userRepository.existsByPhoneNumber(userSummaryDTO.getPhoneNumber())) {
            logWarn("/api/auth/register", "Registration failed - phone already exists: {}",
                    userSummaryDTO.getPhoneNumber());
            return ResponseEntity.badRequest().body("Ошибка: Телефон уже используется!");
        }

        try {
            // Get user role
            Role userRole = roleRepository.findByTitle("Activist")
                    .orElseThrow(() -> new RuntimeException("Ошибка: Роль Activist не найдена в БД!"));

            // Get user group
            Group userGroup = groupRepository.findGroupById(userSummaryDTO.getGroup_id())
                    .orElseThrow(() -> new GroupDoesNotExistException(
                            String.format("Ошибка: Группа с id: %s не существует", userSummaryDTO.getGroup_id())));

            // Get user speciality
            Speciality userSpeciality = specialityRepository.findSpecialityById(userSummaryDTO.getSpeciality_id())
                    .orElseThrow(() -> new SpecialityDoesNotExistException(
                            String.format("Ошибка: Специальность с id: %s не существует",
                                    userSummaryDTO.getSpeciality_id())));

            // Create new user
            User user = new User();
            user.setName(userSummaryDTO.getName());
            user.setSurname(userSummaryDTO.getSurname());
            user.setPatronymic(userSummaryDTO.getPatronymic());
            user.setGender(Gender.valueOf(userSummaryDTO.getGender()));
            user.setDateOfBirth(userSummaryDTO.getDateOfBirth());
            user.setStudentEmail(userSummaryDTO.getStudentEmail());
            user.setPhoneNumber(userSummaryDTO.getPhoneNumber());
            user.setPassword(passwordEncoder.encode(userSummaryDTO.getPassword()));
            user.setStudentIdNumber(userSummaryDTO.getStudentIdNumber());
            user.setCourseNumber(userSummaryDTO.getCourseNumber());
            user.setGroup(userGroup);
            user.setSpeciality(userSpeciality);
            user.setVkLink(userSummaryDTO.getVkLink());
            user.setAdditionalEmail(userSummaryDTO.getAdditionalEmail());
            user.setRole(userRole);
            user.setIsActive(true);
            user.setIsBanned(false);

            User savedUser = userRepository.save(user);

            // Save social statuses if present
            if (userSummaryDTO.getSocial_statuses() != null && !userSummaryDTO.getSocial_statuses().isEmpty()) {
                saveSocialStatuses(savedUser, userSummaryDTO.getSocial_statuses());
            }

            logInfo("/api/auth/register", "User registered successfully with ID: {}", savedUser.getId());

            return createSuccessResponse("Пользователь успешно зарегистрирован!", null);

        } catch (Exception e) {
            logError("/api/auth/register", "Registration failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body("Ошибка при регистрации: " + e.getMessage());
        }
    }

    // Helper method to save social statuses
    private void saveSocialStatuses(User user, List<Long> socialStatusIds) {
        for (Long statusId : socialStatusIds) {
            SocialStatus socialStatus = socialStatusStudentRepository.findById(statusId)
                    .orElseThrow(() -> new RuntimeException("Социальный статус с id " + statusId + " не найден")).getSocialStatus();

            SocialStatusStudent socialStatusStudent = SocialStatusStudent.builder()
                    .student(user)
                    .socialStatus(socialStatus)
                    .build();

            socialStatusStudentRepository.save(socialStatusStudent);
        }
        logInfo("/api/auth/register", "Saved {} social statuses for user ID: {}", socialStatusIds.size(), user.getId());
    }

    @PostMapping("/password-reset/request")
    public ResponseEntity<?> requestPasswordReset(@Valid @RequestBody PasswordResetRequestDTO request) {
        try {
            log.info("Password reset request for email: {}", request.getEmail());
            passwordResetService.requestPasswordReset(request);
            return ResponseEntity.ok(Map.of(
                    "message", "Код для восстановления пароля отправлен на вашу почту",
                    "email", request.getEmail()
            ));
        } catch (Exception e) {
            log.error("Password reset request failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Проверка кода и смена пароля
     * POST /api/auth/password-reset/verify
     */
    @PostMapping("/password-reset/verify")
    public ResponseEntity<?> verifyCodeAndResetPassword(@Valid @RequestBody PasswordResetVerifyDTO request) {
        try {
            log.info("Password reset verification for email: {}", request.getEmail());
            passwordResetService.verifyCodeAndResetPassword(request);
            return ResponseEntity.ok(Map.of(
                    "message", "Пароль успешно изменен"
            ));
        } catch (Exception e) {
            log.error("Password reset verification failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Проверка кода (без смены пароля)
     * POST /api/auth/password-reset/confirm
     */
    @PostMapping("/password-reset/confirm")
    public ResponseEntity<?> confirmCode(@Valid @RequestBody PasswordResetConfirmDTO request) {
        try {
            log.info("Password reset confirm for email: {}", request.getEmail());
            boolean isValid = passwordResetService.verifyCode(request);
            return ResponseEntity.ok(Map.of(
                    "valid", isValid,
                    "message", isValid ? "Код подтвержден" : "Неверный код"
            ));
        } catch (Exception e) {
            log.error("Password reset confirm failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}