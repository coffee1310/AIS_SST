package org.example.ais_sst.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.example.ais_sst.validator.SectorNameValidator;
import org.example.ais_sst.validator.UserEmailExistValidator;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = UserEmailExistValidator.class)
@Documented
public @interface ValidUserEmailExist {

    String message() default "Такой Email уже сущесвует";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
