package org.example.ais_sst.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.example.ais_sst.annotation.ValidUserId;
import org.example.ais_sst.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserIdValidator implements ConstraintValidator<ValidUserId, Long> {

    private final UserRepository userRepository;

    @Override
    public void initialize(ValidUserId validUserId) {

    }

    @Override
    public boolean isValid(Long userId, ConstraintValidatorContext context) {
        if (userId == null) return true;

        boolean exists = userRepository.existsById(userId);

        if(!exists) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    String.format("Пользователь с id=%d не найден", userId)
            ).addConstraintViolation();
        }

        return exists;
    }
}
