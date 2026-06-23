package org.example.ais_sst.dto.event_participant;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoveParticipantDTO {
    @NotNull(message = "ID записи об участии не может быть пустым")
    private Long participationRecordId;

    @NotNull(message = "Статус резерва не может быть пустым")
    private Boolean isReserve;  // true - в резерв, false - в основной состав
}