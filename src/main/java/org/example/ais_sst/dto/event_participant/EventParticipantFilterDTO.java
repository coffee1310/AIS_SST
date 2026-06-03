package org.example.ais_sst.dto.event_participant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventParticipantFilterDTO {
    private Long eventId;
    private Long userId;
    private LocalDateTime joinedFrom;
    private LocalDateTime joinedTo;
}