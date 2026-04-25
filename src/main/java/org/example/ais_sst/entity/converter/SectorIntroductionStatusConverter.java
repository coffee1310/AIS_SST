package org.example.ais_sst.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.example.ais_sst.entity.enums.SectorIntroductionStatus;

@Converter(autoApply = true)
public class SectorIntroductionStatusConverter implements AttributeConverter<SectorIntroductionStatus, String> {

    @Override
    public String convertToDatabaseColumn(SectorIntroductionStatus attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getDbValue(); // Это вернет "На рассмотрении" вместо "НА_РАССМОТРЕНИИ"
    }

    @Override
    public SectorIntroductionStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return SectorIntroductionStatus.fromDbValue(dbData);
    }

}
