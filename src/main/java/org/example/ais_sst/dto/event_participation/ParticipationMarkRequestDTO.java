package org.example.ais_sst.dto.event_participation;

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
public class ParticipationMarkRequestDTO {

    @NotNull(message = "ID мероприятия обязателен")
    private Long eventId;

    private List<Long> participantIds;      // ID участников для отметки

    private List<Long> organizerIds;        // ID организаторов для отметки

    private List<Long> participationRecordIds;  // ID записей event_participation_records

    @Builder.Default
    private Boolean present = true;         // true - отметить как присутствовавших, false - убрать отметку

    private String comment;                 // Комментарий (опционально)
}