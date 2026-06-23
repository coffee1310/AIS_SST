package org.example.ais_sst.dto.user;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserRating {
    private Integer totalPoints;
    private Integer eventsCount;
    private Integer tasksCount;
    private Integer participationPoints;
    private Integer organizerPoints;
    private Integer participantPoints;
    private Integer taskPoints;
}