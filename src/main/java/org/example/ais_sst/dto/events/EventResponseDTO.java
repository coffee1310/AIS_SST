package org.example.ais_sst.dto.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
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
    private LocalDate dateOfEvent;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String venue;  // Изменено с location на venue
    private Long eventCreatorId;
    private String referenceToPosition;
    private String eventCreatorName;
    private String eventCreatorSurname;
    private Boolean isActive;
    private Boolean isPublic;  // Добавлено поле isPublic
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<EventOrganizerResponseDTO> organizers;
}