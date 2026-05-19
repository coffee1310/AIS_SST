package org.example.ais_sst.dto.event_roles_application;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.ais_sst.entity.enums.RoleApplicationStatuses;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "DTO для ответа с информацией о заявке на организатора")
@AllArgsConstructor
@NoArgsConstructor
public class EventOrganizerRequestResponseDTO {

    @Schema(description = "ID заявки", example = "1")
    private Long id;

    @Schema(description = "ID пользователя", example = "10")
    private Long userId;

    @Schema(description = "Имя пользователя", example = "Иван")
    private String userName;

    @Schema(description = "Фамилия пользователя", example = "Иванов")
    private String userSurname;

    @Schema(description = "Email пользователя", example = "ivan@example.com")
    private String userEmail;

    @Schema(description = "ID мероприятия", example = "5")
    private Long eventId;

    @Schema(description = "Название мероприятия", example = "Java Conference 2026")
    private String eventTitle;

    @Schema(description = "Дата и время создания заявки")
    private LocalDateTime createdAt;

    @Schema(description = "Статус заявки", example = "НА_РАССМОТРЕНИИ")
    private RoleApplicationStatuses status;
}