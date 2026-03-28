package org.example.ais_sst.converter;

import org.example.ais_sst.entity.enums.Gender;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class GenderConverter implements AttributeConverter<Gender, String> {

    @Override
    public String convertToDatabaseColumn(Gender gender) {
        if (gender == null) return null;
        return gender.name(); // Вернет "Мужчина" или "Женщина"
    }

    @Override
    public Gender convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            return Gender.valueOf(dbData);
        } catch (IllegalArgumentException e) {
            System.err.println("Unknown gender value in database: " + dbData);
            return null;
        }
    }
}