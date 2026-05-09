package org.example.ais_sst.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum SectorParticipantStatuses {

    Ожидание("Ожидание"),
    Активный("Активный"),
    Вышедший("Вышедший");

    private final String dbValue;

    SectorParticipantStatuses(String dbValue) {
        this.dbValue = dbValue;
    }

    @JsonCreator
    public static SectorParticipantStatuses fromString(String value) {
        if (value == null) return null;

        // Прямое совпадение с русскими названиями
        for (SectorParticipantStatuses status : SectorParticipantStatuses.values()) {
            if (status.name().equals(value)) {
                return status;
            }
        }

        throw new IllegalArgumentException("Unknown status: " + value);
    }

    @JsonValue
    public String getDbValue() {
        return dbValue;
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}
