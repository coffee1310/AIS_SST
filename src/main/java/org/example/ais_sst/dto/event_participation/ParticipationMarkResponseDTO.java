package org.example.ais_sst.dto.event_participation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParticipationMarkResponseDTO {
    private Long eventId;
    private Integer markedParticipants;
    private Integer markedOrganizers;
    private List<Long> participationRecordIds;  // ID записей из event_participation_records
    private Integer totalPointsAwarded;
    private String message;
    private List<MarkedEntityDTO> details;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MarkedEntityDTO {
        private Long id;
        private String type;      // PARTICIPANT, ORGANIZER, EVENT_ROLE
        private String name;
        private Integer pointsAwarded;
        private Boolean wasPresent;
    }
}