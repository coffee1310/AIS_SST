package org.example.ais_sst.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SectorIntroductionStatus {
    Ожидание, Активный, Вышедший;

    @JsonCreator
    public static SectorIntroductionStatus fromString(String value) {
        if (value == null) return null;

        // Прямое совпадение с русскими названиями
        for (SectorIntroductionStatus status : SectorIntroductionStatus.values()) {
            if (status.name().equals(value)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unknown gender: " + value);
    }

    @JsonValue
    public String toValue() {
        return this.name(); // Возвращает "Мужчина" или "Женщина"
    }
}