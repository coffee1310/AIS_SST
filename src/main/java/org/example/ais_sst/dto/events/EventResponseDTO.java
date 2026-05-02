package org.example.ais_sst.dto.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventResponseDTO {

    private Long id;
    private String title;
    private String description;
    private String photo;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String location;
    private Long eventCreatorId;
    private String eventCreatorName;
    private String eventCreatorSurname;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<EventOrganizerResponseDTO> organizers;
}