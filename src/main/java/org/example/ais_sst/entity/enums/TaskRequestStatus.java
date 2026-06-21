package org.example.ais_sst.entity.enums;

public enum TaskRequestStatus {
    НА_РАССМОТРЕНИИ("На рассмотрении"),
    ПРИНЯТО("Принято"),
    ОТКЛОНЕНО("Отклонено");

    private final String displayName;

    TaskRequestStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static TaskRequestStatus fromDisplayName(String displayName) {
        for (TaskRequestStatus status : TaskRequestStatus.values()) {
            if (status.getDisplayName().equals(displayName)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + displayName);
    }
}