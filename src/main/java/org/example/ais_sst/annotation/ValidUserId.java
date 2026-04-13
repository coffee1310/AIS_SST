package org.example.ais_sst.annotation;


import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.example.ais_sst.validator.UserIdValidator;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UserIdValidator.class)
@Documented
public @interface ValidUserId {

    String message() default "Пользователь с таким id не найден";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
