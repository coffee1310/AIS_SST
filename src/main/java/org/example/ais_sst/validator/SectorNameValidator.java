package org.example.ais_sst.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.example.ais_sst.annotation.ValidSectorName;
import org.example.ais_sst.repository.SectorRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SectorNameValidator implements ConstraintValidator<ValidSectorName, String> {

    private final SectorRepository sectorRepository;

    @Override
    public void initialize(ValidSectorName validUserId) {

    }

    @Override
    public boolean isValid(String title, ConstraintValidatorContext context) {
        if (title == null) return true;

        boolean exists = sectorRepository.existsByTitle(title);

        if(exists) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                    String.format("Сектор с названием '%s' уже существует", title)
            ).addConstraintViolation();
            return false;
        }

        return true;
    }
}
