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
            return "Ожидание";
        }
        switch (attribute) {
            case Ожидание:
                return "Ожидание";
            case Активный:
                return "Активный";
            case Вышедший:
                return "Вышедший";
            default:
                return "Ожидание";
        }
    }

    @Override
    public SectorParticipantStatuses convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return SectorParticipantStatuses.Ожидание;
        }
        switch (dbData) {
            case "Ожидание":
                return SectorParticipantStatuses.Ожидание;
            case "Активный":
                return SectorParticipantStatuses.Активный;
            case "Вышел":
                return SectorParticipantStatuses.Вышедший;
            default:
                return SectorParticipantStatuses.Ожидание;
        }
    }
}