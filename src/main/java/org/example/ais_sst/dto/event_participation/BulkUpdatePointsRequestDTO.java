package org.example.ais_sst.dto.event_participation;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUpdatePointsRequestDTO {

    @NotNull(message = "ID мероприятия обязателен")
    private Long eventId;

    private List<Long> participantIds;

    private List<Long> organizerIds;

    private List<Long> participationRecordIds;

    @NotNull(message = "Количество баллов обязательно")
    @Min(value = 0, message = "Баллы не могут быть отрицательными")
    private Integer points;

    private String reason;
}