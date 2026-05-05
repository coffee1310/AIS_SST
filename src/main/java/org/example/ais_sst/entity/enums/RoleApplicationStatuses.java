package org.example.ais_sst.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RoleApplicationStatuses {
    ОЖИДАНИЕ("Отклонено"),
    НА_РАССМОТРЕНИИ("На рассмотрении"),
    ОДОБРЕНА("Принято");

    private final String dbValue;

    RoleApplicationStatuses(String dbValue) {
        this.dbValue = dbValue;
    }

    @JsonValue
    public String getDbValue() {
        return dbValue;
    }

    @JsonCreator
    public static RoleApplicationStatuses fromDbValue(String value) {
        for (RoleApplicationStatuses status : values()) {
            if (status.dbValue.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + value);
    }
}
