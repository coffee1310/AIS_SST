package org.example.ais_sst.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.example.ais_sst.annotation.ValidSectorName;
import org.example.ais_sst.dto.sector.SectorDTO;
import org.example.ais_sst.repository.SectorRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SectorNameValidator implements ConstraintValidator<ValidSectorName, Object> {

    private final SectorRepository sectorRepository;

    @Override
    public void initialize(ValidSectorName constraintAnnotation) {
        // Инициализация не требуется
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        String title;
        Long sectorId;

        // Если валидируем поле String (при создании)
        if (value instanceof String) {
            title = (String) value;
            sectorId = null;
        }
        // Если валидируем весь DTO (при обновлении)
        else if (value instanceof SectorDTO) {
            SectorDTO dto = (SectorDTO) value;
            title = dto.getTitle();
            sectorId = dto.getId();
        } else {
            title = null;
            sectorId = null;
        }

        if (title == null || title.isEmpty()) {
            return true;
        }

        // Проверяем существование сектора с таким названием
        return sectorRepository.findByTitle(title)
                .map(existingSector -> {
                    // Если это тот же сектор (обновление) - пропускаем
                    if (sectorId != null && existingSector.getId().equals(sectorId)) {
                        return true;
                    }
                    // Иначе - название уже занято
                    context.disableDefaultConstraintViolation();
                    context.buildConstraintViolationWithTemplate(
                            String.format("Сектор с названием '%s' уже существует", title)
                    ).addConstraintViolation();
                    return false;
                })
                .orElse(true); // Если сектор не найден - название свободно
    }
}