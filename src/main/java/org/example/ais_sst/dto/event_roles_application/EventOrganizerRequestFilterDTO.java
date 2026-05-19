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
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO для фильтрации заявок на организатора")
public class EventOrganizerRequestFilterDTO {

    @Schema(description = "ID пользователя", example = "10")
    private Long userId;

    @Schema(description = "ID мероприятия", example = "5")
    private Long eventId;

    @Schema(description = "Статус заявки", example = "PENDING")
    private RoleApplicationStatuses status;

    @Schema(description = "Дата создания от")
    private LocalDateTime dateFrom;

    @Schema(description = "Дата создания до")
    private LocalDateTime dateTo;
}