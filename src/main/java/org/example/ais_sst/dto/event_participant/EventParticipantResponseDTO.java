package org.example.ais_sst.dto.event_participant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventParticipantResponseDTO {
    private Long id;
    private Long eventId;
    private String eventTitle;
    private String eventDescription;
    private LocalDate eventDateOfEvent;
    private LocalTime eventStartTime;
    private LocalTime eventEndTime;
    private String eventVenue;
    private Long userId;
    private String studentName;
    private String studentSurname;
    private String studentPatronymic;
    private String studentEmail;
    private LocalDateTime joinedAt;
}
