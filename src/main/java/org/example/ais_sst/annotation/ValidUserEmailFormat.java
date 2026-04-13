package org.example.ais_sst.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.example.ais_sst.validator.EmailFormatValidator;
import org.hibernate.validator.internal.constraintvalidators.bv.EmailValidator;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = EmailFormatValidator.class)
@Documented
public @interface ValidUserEmailFormat {

    String message() default "Некорректный формат email. Допустимые домены: @fa.ru или @edu.fa.ru";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    // Опционально: можно указать разрешённые домены по умолчанию
    String[] allowedDomains() default {"fa.ru", "edu.fa.ru"};

    // Строгая проверка формата RFC 5322
    boolean strict() default true;
}
