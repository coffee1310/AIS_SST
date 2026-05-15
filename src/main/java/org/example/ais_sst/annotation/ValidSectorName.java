package org.example.ais_sst.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import org.example.ais_sst.validator.SectorNameValidator;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SectorNameValidator.class)
@Documented
public @interface ValidSectorName {

    String message() default "Сектор с таким именем уже существует";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}