package org.example.ais_sst.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.example.ais_sst.entity.enums.Gender;
import org.example.ais_sst.entity.enums.RoleApplicationStatuses;
import org.springframework.stereotype.Component;

@Converter(autoApply = true)
public class RoleApplicationStatusesConverter implements AttributeConverter<RoleApplicationStatuses, String> {
    @Override
    public String convertToDatabaseColumn(RoleApplicationStatuses roleApplicationStatuses) {
        if (roleApplicationStatuses == null) return null;
        return roleApplicationStatuses.name();
    }

    @Override
    public RoleApplicationStatuses convertToEntityAttribute(String s) {
        if (s == null) return null;
        return RoleApplicationStatuses.valueOf(s);  // Из строки в enum
    }
}
