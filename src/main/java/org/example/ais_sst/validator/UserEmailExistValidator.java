package org.example.ais_sst.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.example.ais_sst.annotation.ValidSectorName;
import org.example.ais_sst.annotation.ValidUserEmailExist;
import org.example.ais_sst.repository.SectorRepository;
import org.example.ais_sst.repository.UserRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserEmailExistValidator implements ConstraintValidator<ValidUserEmailExist, String> {

    private final UserRepository userRepository;

    @Override
    public void initialize(ValidUserEmailExist validUserEmailExist) {
    }

    @Override
    public boolean isValid(String email, ConstraintValidatorContext context) {
        if (email == null) return true;

        boolean exists = userRepository.existsByStudentEmail(email);

        if(exists) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    String.format("Email '%s' уже существует", email)
            ).addConstraintViolation();
            return false;
        }

        return true;
    }
}