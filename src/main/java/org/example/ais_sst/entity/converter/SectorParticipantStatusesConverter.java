package org.example.ais_sst.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.entity.enums.SectorParticipantStatuses;

@Slf4j
@Converter(autoApply = true)
public class SectorParticipantStatusesConverter implements AttributeConverter<SectorParticipantStatuses, String> {

    @Override
    public String convertToDatabaseColumn(SectorParticipantStatuses attribute) {
        if (attribute == null) {
            return null;
        }
        // Возвращаем строковое значение для БД
        return attribute.getDbValue();
    }

    @Override
    public SectorParticipantStatuses convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        // Преобразуем из строки БД в enum
        return SectorParticipantStatuses.fromString(dbData);
    }
}