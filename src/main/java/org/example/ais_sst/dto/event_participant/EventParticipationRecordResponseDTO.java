package org.example.ais_sst.dto.event_participant;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EventParticipationRecordResponseDTO {
    private Long id;
    private Long sectorParticipantId;
    private String sectorParticipantStatus;
    private Long eventRoleId;
    private String eventRoleTitle;
    private Long eventId;
    private String eventTitle;
    private Long userId;
    private String userFullName;
    private String userEmail;
    private Boolean wasPresent;
    private Boolean isReserve;
    private Integer totalPoints;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}