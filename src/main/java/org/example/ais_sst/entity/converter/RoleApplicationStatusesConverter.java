package org.example.ais_sst.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.example.ais_sst.entity.enums.Gender;
import org.example.ais_sst.entity.enums.RoleApplicationStatuses;
import org.springframework.stereotype.Component;

@Converter(autoApply = true)
public class RoleApplicationStatusesConverter implements AttributeConverter<RoleApplicationStatuses, String> {

    @Override
    public String convertToDatabaseColumn(RoleApplicationStatuses attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getDbValue();
    }

    @Override
    public RoleApplicationStatuses convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return RoleApplicationStatuses.fromDbValue(dbData);
    }
}