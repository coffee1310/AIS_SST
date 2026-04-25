package org.example.ais_sst.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SectorIntroductionStatus {
    ОЖИДАНИЕ("Ожидание"),
    НА_РАССМОТРЕНИИ("На рассмотрении"),
    ОДОБРЕНА("Одобрена"),
    ОТКЛОНЕНА("Отклонена"),
    ВЫШЕДШИЙ("Вышедший");

    private final String dbValue;

    SectorIntroductionStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @JsonValue
    public String getDbValue() {
        return dbValue;
    }

    @JsonCreator
    public static SectorIntroductionStatus fromDbValue(String value) {
        for (SectorIntroductionStatus status : values()) {
            if (status.dbValue.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + value);
    }
}