package org.example.ais_sst.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Gender {
    Мужчина, Женщина;

    @JsonCreator
    public static Gender fromString(String value) {
        if (value == null) return null;

        // Прямое совпадение с русскими названиями
        for (Gender gender : Gender.values()) {
            if (gender.name().equals(value)) {
                return gender;
            }
        }

        // Поддержка английских вариантов (на всякий случай)
        if (value.equalsIgnoreCase("MALE") || value.equalsIgnoreCase("male")) {
            return Мужчина;
        }
        if (value.equalsIgnoreCase("FEMALE") || value.equalsIgnoreCase("female")) {
            return Женщина;
        }

        throw new IllegalArgumentException("Unknown gender: " + value);
    }

    @JsonValue
    public String toValue() {
        return this.name(); // Возвращает "Мужчина" или "Женщина"
    }
}