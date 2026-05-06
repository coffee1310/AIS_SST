package org.example.ais_sst.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AccountCreatingRequestStatus {
    ОТКЛОНЕНА("Отклонено"),
    НА_РАССМОТРЕНИИ("На рассмотрении"),
    ОДОБРЕНА("Принято");

    private final String dbValue;

    AccountCreatingRequestStatus(String dbValue) {
        this.dbValue = dbValue;
    }

    @JsonValue
    public String getDbValue() {
        return dbValue;
    }

    @JsonCreator
    public static AccountCreatingRequestStatus fromDbValue(String value) {
        for (AccountCreatingRequestStatus status : values()) {
            if (status.dbValue.equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + value);
    }
}