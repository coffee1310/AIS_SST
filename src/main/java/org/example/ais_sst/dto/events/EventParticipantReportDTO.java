package org.example.ais_sst.dto.events;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EventParticipantReportDTO {
    private Long userId;
    private String fio;
    private String groupName;
    private Short courseNumber;
    private Integer age;
    private Boolean wasPresent;
    private Integer pointsReceived;
}
