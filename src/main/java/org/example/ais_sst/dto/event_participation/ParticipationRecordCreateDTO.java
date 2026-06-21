package org.example.ais_sst.dto.event_participation;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipationRecordCreateDTO {

    @NotNull(message = "ID участника сектора обязателен")
    private Long sectorParticipantId;

    @NotNull(message = "ID роли мероприятия обязателен")
    private Long eventRoleId;

    private String comment;
}