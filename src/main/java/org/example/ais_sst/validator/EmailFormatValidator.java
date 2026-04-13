package org.example.ais_sst.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.example.ais_sst.annotation.ValidUserEmailFormat;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class EmailFormatValidator implements ConstraintValidator<ValidUserEmailFormat, String> {

    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@(.+)$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    private String[] allowedDomains;
    private boolean strict;

    @Override
    public void initialize(ValidUserEmailFormat constraintAnnotation) {
        this.allowedDomains = constraintAnnotation.allowedDomains();
        this.strict = constraintAnnotation.strict();
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        // Если email null - пропускаем (проверяем через @NotNull отдельно)
        if (email == null) {
            return true;
        }

        // Базовый формат email
        if (!isValidEmailFormat(email)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Некорректный формат email. Пример: user@fa.ru"
            ).addConstraintViolation();
            return false;
        }

        // Проверка домена
        String domain = extractDomain(email);
        if (!isValidDomain(domain)) {
            String allowedDomainsStr = String.join(" или @", allowedDomains);
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    String.format("Домен '%s' не разрешён. Допустимые домены: @%s",
                            domain, allowedDomainsStr)
            ).addConstraintViolation();
            return false;
        }

        // Дополнительная строгая проверка
        if (strict && !isStrictlyValid(email)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    "Email содержит недопустимые символы"
            ).addConstraintViolation();
            return false;
        }

        return true;
    }

    // Проверка базового формата email
    private boolean isValidEmailFormat(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }

        // Простая проверка на наличие @ и точки после @
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            return false;
        }

        String[] parts = email.split("@");
        if (parts.length != 2) {
            return false;
        }

        String localPart = parts[0];
        String domainPart = parts[1];

        // Локальная часть не должна быть пустой
        if (localPart.isEmpty() || localPart.length() > 64) {
            return false;
        }

        // Домен не должен быть пустым
        if (domainPart.isEmpty() || domainPart.length() > 255) {
            return false;
        }

        return true;
    }

    // Извлечение домена из email
    private String extractDomain(String email) {
        String[] parts = email.split("@");
        if (parts.length == 2) {
            return parts[1];
        }
        return "";
    }

    // Проверка, что домен в списке разрешённых
    private boolean isValidDomain(String domain) {
        return Arrays.asList(allowedDomains).contains(domain.toLowerCase());
    }

    // Строгая проверка формата (дополнительные правила)
    private boolean isStrictlyValid(String email) {
        // Более строгая проверка: только латиница, цифры, точки, дефисы, подчёркивания
        String strictRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return Pattern.matches(strictRegex, email);
    }
}