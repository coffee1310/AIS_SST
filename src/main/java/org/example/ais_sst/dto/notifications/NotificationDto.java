package org.example.ais_sst.dto.notifications;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto implements Serializable {
    private String id;
    private String userId;
    private String message;
    private String type; // INFO, WARNING, ERROR
    private LocalDateTime timestamp;
    private Object data; // Дополнительные данные

    public NotificationDto(String userId, String message, String type) {
        this.id = java.util.UUID.randomUUID().toString();
        this.userId = userId;
        this.message = message;
        this.type = type;
        this.timestamp = LocalDateTime.now();
    }
}