package org.example.ais_sst.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.response.ErrorResponse;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================== 1. Ошибки валидации ====================

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(
            MethodArgumentNotValidException ex, WebRequest request) {

        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Validation Failed")
                .message("Ошибка валидации входных данных")
                .details(errors)
                .path(request.getDescription(false))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException ex, WebRequest request) {

        Map<String, String> errors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> {
                            String path = violation.getPropertyPath().toString();
                            return path.substring(path.lastIndexOf('.') + 1);
                        },
                        ConstraintViolation::getMessage,
                        (error1, error2) -> error1 + ", " + error2
                ));

        ErrorResponse response = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error("Constraint Violation")
                .message("Нарушение ограничений валидации")
                .details(errors)
                .path(request.getDescription(false))
                .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    // ==================== 2. Ошибки пользователей ====================

    @ExceptionHandler(UserDoesNotExistException.class)
    public ResponseEntity<Map<String, Object>> handleUserDoesNotExist(
            UserDoesNotExistException ex, WebRequest request) {
        log.error("User not found: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Пользователь не найден",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleEmailAlreadyExists(
            EmailAlreadyExistsException ex, WebRequest request) {
        log.error("Email already exists: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "Email уже существует",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(PhoneAlreadyExistException.class)
    public ResponseEntity<Map<String, Object>> handlePhoneAlreadyExist(
            PhoneAlreadyExistException ex, WebRequest request) {
        log.error("Phone already exists: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "Телефон уже существует",
                ex.getMessage(),
                request
        );
    }

    // ==================== 3. Ошибки сущностей ====================

    @ExceptionHandler(GroupDoesNotExistException.class)
    public ResponseEntity<Map<String, Object>> handleGroupDoesNotExist(
            GroupDoesNotExistException ex, WebRequest request) {
        log.error("Group not found: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Группа не найдена",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(SpecialityDoesNotExistException.class)
    public ResponseEntity<Map<String, Object>> handleSpecialityDoesNotExist(
            SpecialityDoesNotExistException ex, WebRequest request) {
        log.error("Speciality not found: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Специальность не найдена",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleRoleNotFound(
            RoleNotFoundException ex, WebRequest request) {
        log.error("Role not found: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Роль не найдена",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(RoleAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleRoleAlreadyExists(
            RoleAlreadyExistsException ex, WebRequest request) {
        log.error("Role already exists: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "Роль уже существует",
                ex.getMessage(),
                request
        );
    }

    // ==================== 4. Ошибки секторов ====================

    @ExceptionHandler(SectorDoesNotExistException.class)
    public ResponseEntity<Map<String, Object>> handleSectorDoesNotExist(
            SectorDoesNotExistException ex, WebRequest request) {
        log.error("Sector not found: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Сектор не найден",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(SectorParticipantNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleSectorParticipantNotFound(
            SectorParticipantNotFoundException ex, WebRequest request) {
        log.error("Sector participant not found: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Участник сектора не найден",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(NoSectorWithSuchCooridnatorFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNoSectorWithSuchCooridnatorFound(
            NoSectorWithSuchCooridnatorFoundException ex, WebRequest request) {
        log.error("Sector coordinator not found: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Координатор сектора не найден",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(UserIsAlreadyInThisSectorException.class)
    public ResponseEntity<Map<String, Object>> handleUserIsAlreadyInThisSector(
            UserIsAlreadyInThisSectorException ex, WebRequest request) {
        log.error("User is already in this sector: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "Пользователь уже в этом секторе",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(SectorIntroductionRequestAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleSectorIntroductionRequestAlreadyExists(
            SectorIntroductionRequestAlreadyExistsException ex, WebRequest request) {
        log.error("Sector introduction request already exists: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "Заявка на вступление в сектор уже существует",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(SectorIntroductionRequestAlreadyProcessedException.class)
    public ResponseEntity<Map<String, Object>> handleSectorIntroductionRequestAlreadyProcessed(
            SectorIntroductionRequestAlreadyProcessedException ex, WebRequest request) {
        log.error("Sector introduction request already processed: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Заявка на вступление уже обработана",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(SectorIntroductionRequestDoesNotExistException.class)
    public ResponseEntity<Map<String, Object>> handleSectorIntroductionRequestDoesNotExist(
            SectorIntroductionRequestDoesNotExistException ex, WebRequest request) {
        log.error("Sector introduction request not found: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Заявка на вступление в сектор не найдена",
                ex.getMessage(),
                request
        );
    }

    // ==================== 5. Ошибки заявок на аккаунт ====================

    @ExceptionHandler(AccountCreatingRequestDoesNotExistException.class)
    public ResponseEntity<Map<String, Object>> handleAccountCreatingRequestDoesNotExist(
            AccountCreatingRequestDoesNotExistException ex, WebRequest request) {
        log.error("Account creating request not found: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Заявка на создание аккаунта не найдена",
                ex.getMessage(),
                request
        );
    }

    // ==================== 6. Ошибки заявок на роль ====================

    @ExceptionHandler(ApplicationDoesNotExistException.class)
    public ResponseEntity<Map<String, Object>> handleApplicationDoesNotExist(
            ApplicationDoesNotExistException ex, WebRequest request) {
        log.error("Application not found: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Заявка не найдена",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(DuplicateApplicationException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateApplication(
            DuplicateApplicationException ex, WebRequest request) {
        log.error("Duplicate application: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "Дубликат заявки",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(DuplicateParticipationRecordException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicateParticipationRecord(
            DuplicateParticipationRecordException ex, WebRequest request) {
        log.error("Duplicate participation record: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "Дубликат записи об участии",
                ex.getMessage(),
                request
        );
    }

    // ==================== 7. Ошибки событий и ролей ====================

    @ExceptionHandler(EventDoesNotExistException.class)
    public ResponseEntity<Map<String, Object>> handleEventDoesNotExist(
            EventDoesNotExistException ex, WebRequest request) {
        log.error("Event not found: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Мероприятие не найдено",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(EventRoleDoesNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleEventRoleNotFound(
            EventRoleDoesNotFoundException ex, WebRequest request) {
        log.error("Event role not found: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Роль мероприятия не найдена",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(EventRoleAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleEventRoleAlreadyExists(
            EventRoleAlreadyExistsException ex, WebRequest request) {
        log.error("Event role already exists: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "Роль мероприятия уже существует",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(GlobalRoleDoesNotExistException.class)
    public ResponseEntity<Map<String, Object>> handleGlobalRoleDoesNotExist(
            GlobalRoleDoesNotExistException ex, WebRequest request) {
        log.error("Global role not found: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Глобальная роль не найдена",
                ex.getMessage(),
                request
        );
    }

    // ==================== 8. Ошибки организаторов ====================

    @ExceptionHandler(OrganizerDoesNotExistException.class)
    public ResponseEntity<Map<String, Object>> handleOrganizerDoesNotExist(
            OrganizerDoesNotExistException ex, WebRequest request) {
        log.error("Organizer not found: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Организатор не найден",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(UserAlreadyOrganizerException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyOrganizer(
            UserAlreadyOrganizerException ex, WebRequest request) {
        log.error("User is already organizer: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "Пользователь уже организатор",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(EventOrganizerRequestAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleEventOrganizerRequestAlreadyExists(
            EventOrganizerRequestAlreadyExistsException ex, WebRequest request) {
        log.error("Organizer request already exists: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "Заявка на организатора уже подана",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(OrganizerLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleOrganizerLimitExceeded(
            OrganizerLimitExceededException ex, WebRequest request) {
        log.error("Organizer limit exceeded: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Превышен лимит организаторов",
                ex.getMessage(),
                request
        );
    }

    // ==================== 9. Ошибки социальных статусов ====================

    @ExceptionHandler(SocialStatusDoesNotExistException.class)
    public ResponseEntity<Map<String, Object>> handleSocialStatusDoesNotExist(
            SocialStatusDoesNotExistException ex, WebRequest request) {
        log.error("Social status not found: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Социальный статус не найден",
                ex.getMessage(),
                request
        );
    }

    // ==================== 10. Ошибки безопасности и токенов ====================

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorizedException(
            UnauthorizedException ex, WebRequest request) {
        log.error("Unauthorized: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "Неавторизован",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        log.error("Access denied: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.FORBIDDEN,
                "Доступ запрещен",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(TokenRefreshException.class)
    public ResponseEntity<Map<String, Object>> handleTokenRefreshException(
            TokenRefreshException ex, WebRequest request) {
        log.error("Token refresh error: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "Ошибка обновления токена",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(TokenIsNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleTokenIsNotValidException(
            TokenIsNotValidException ex, WebRequest request) {
        log.error("Invalid token: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "Невалидный токен",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(io.jsonwebtoken.ExpiredJwtException.class)
    public ResponseEntity<Map<String, Object>> handleExpiredJwtException(
            io.jsonwebtoken.ExpiredJwtException ex, WebRequest request) {
        log.error("JWT expired: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "Токен истек",
                "Токен истек. Пожалуйста, авторизуйтесь заново",
                request
        );
    }

    @ExceptionHandler(io.jsonwebtoken.MalformedJwtException.class)
    public ResponseEntity<Map<String, Object>> handleMalformedJwtException(
            io.jsonwebtoken.MalformedJwtException ex, WebRequest request) {
        log.error("Malformed JWT: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "Неверный формат токена",
                "Неверный формат токена",
                request
        );
    }

    @ExceptionHandler(io.jsonwebtoken.security.SignatureException.class)
    public ResponseEntity<Map<String, Object>> handleSignatureException(
            io.jsonwebtoken.security.SignatureException ex, WebRequest request) {
        log.error("Invalid signature: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "Неверная подпись токена",
                "Неверная подпись токена",
                request
        );
    }

    // ==================== 11. Ошибки верификации и пароля ====================

    @ExceptionHandler(VerificationCodeException.class)
    public ResponseEntity<Map<String, Object>> handleVerificationCodeException(
            VerificationCodeException ex, WebRequest request) {
        log.error("Verification code error: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Ошибка верификации",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(PasswordResetException.class)
    public ResponseEntity<Map<String, Object>> handlePasswordResetException(
            PasswordResetException ex, WebRequest request) {
        log.error("Password reset error: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Ошибка сброса пароля",
                ex.getMessage(),
                request
        );
    }

    // ==================== 12. Ошибки базы данных ====================

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, WebRequest request) {
        log.error("Data integrity violation: {}", ex.getMessage());

        String message = "Нарушение целостности данных";
        String exMessage = ex.getMostSpecificCause().getMessage();

        if (exMessage.contains("total_points")) {
            message = "Не указано количество баллов для записи об участии";
        } else if (exMessage.contains("unique constraint") || exMessage.contains("duplicate key")) {
            message = "Запись с такими данными уже существует";
        } else if (exMessage.contains("student_id_number")) {
            message = "Студент с таким номером студенческого билета уже существует";
        } else if (exMessage.contains("student_email")) {
            message = "Студент с таким email уже существует";
        } else if (exMessage.contains("phone_number")) {
            message = "Студент с таким номером телефона уже существует";
        }

        return buildErrorResponse(
                HttpStatus.CONFLICT,
                "Нарушение целостности данных",
                message,
                request
        );
    }

    // ==================== 13. Общие ошибки ====================

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(
            IllegalStateException ex, WebRequest request) {
        log.error("Illegal state: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Некорректный запрос",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex, WebRequest request) {
        log.error("Illegal argument: {}", ex.getMessage());
        return buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Некорректный аргумент",
                ex.getMessage(),
                request
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, WebRequest request) {
        log.error("Unexpected error: {}", ex.getMessage(), ex);
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Внутренняя ошибка сервера",
                "Произошла непредвиденная ошибка. Пожалуйста, попробуйте позже.",
                request
        );
    }

    // ==================== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ====================

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status,
            String error,
            Object message,
            WebRequest request) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", status.value());
        response.put("error", error);
        response.put("message", message);
        response.put("path", request != null ? request.getDescription(false) : null);
        return ResponseEntity.status(status).body(response);
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status,
            String error,
            Object message) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", status.value());
        response.put("error", error);
        response.put("message", message);
        return ResponseEntity.status(status).body(response);
    }
}