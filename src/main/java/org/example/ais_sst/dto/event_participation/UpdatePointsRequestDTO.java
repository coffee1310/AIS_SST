package org.example.ais_sst.dto.event_participation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePointsRequestDTO {

    @NotNull(message = "ID сущности обязателен")
    private Long entityId;

    @NotNull(message = "Тип сущности обязателен")
    private EntityType entityType;  // PARTICIPANT, ORGANIZER, EVENT_ROLE

    @NotNull(message = "Количество баллов обязательно")
    @Min(value = 0, message = "Баллы не могут быть отрицательными")
    private Integer points;

    private String reason;  // Причина изменения (опционально)

    public enum EntityType {
        PARTICIPANT,
        ORGANIZER,
        PARTICIPATION_RECORD
    }
}