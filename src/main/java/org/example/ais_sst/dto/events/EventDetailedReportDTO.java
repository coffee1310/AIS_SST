package org.example.ais_sst.dto.events;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class EventDetailedReportDTO {
    private Long eventId;
    private String title;
    private LocalDate dateOfEvent;
    private Boolean isCompleted;
    private Boolean isPublic;
    private Boolean isFreeEvent;

    private Integer totalPeopleCount;
    private Integer totalOrganizersCount;
    private Integer totalParticipantsCount;
    private Integer totalPerformersCount;

    private List<EventParticipantReportDTO> participants;
    private List<EventOrganizerReportDTO> organizers;
    private List<EventPerformerReportDTO> performers;
    private List<EventRoleSummaryDTO> roles;
}