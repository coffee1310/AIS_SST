package org.example.ais_sst.dto.event_participation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateParticipationRecordResponse {
    private Long id;
    private Long sectorParticipantId;
    private String sectorParticipantName;
    private Long eventRoleId;
    private String eventRoleName;
    private String comment;
    private Integer totalPoints;
    private Boolean wasPresent;
    private Boolean wasRestored; // true если запись была восстановлена, false если создана новая
}