package org.example.ais_sst.dto.user;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
public class UserParticipationInfoDTO {
    private Long recordId;
    private Long eventId;
    private String eventTitle;
    private String roleTitle;
    private Integer points;
    private Boolean wasPresent;
    private Boolean eventCompleted;

    public UserParticipationInfoDTO(Long recordId, Long eventId, String eventTitle,
                                    String roleTitle, Integer points, Boolean wasPresent,
                                    Boolean eventCompleted) {
        this.recordId = recordId;
        this.eventId = eventId;
        this.eventTitle = eventTitle;
        this.roleTitle = roleTitle;
        this.points = points;
        this.wasPresent = wasPresent;
        this.eventCompleted = eventCompleted;
    }
}