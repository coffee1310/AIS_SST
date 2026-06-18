package org.example.ais_sst.dto.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    private LocalTime startTime;
    private LocalTime endTime;
    private String venue;  // Изменено с location на venue
    private Long eventCreatorId;
    private String referenceToPosition;
    private String eventCreatorName;
    private String eventCreatorSurname;
    private Boolean isActive;
    private Boolean isPublic;  // Добавлено поле isPublic
    private Boolean isDraft;      // Добавлено
    private Boolean isCompleted;  // Добавлено
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<EventOrganizerResponseDTO> organizers;
    private Boolean isFreeEvent = true;  // true - можно участвовать, false - нельзя участвовать
    private Boolean isDeleted = false;


    private Integer maxParticipantsCount = 0;

    private Integer maxOrganizersCount = 0;

    private Long currentParticipantsCount;   // Текущее количество участников
    private Long currentOrganizersCount;     // Текущее количество организаторов

    private List<EventSectorResponseDTO> sectors;
    private String sectorTitle;

    // НОВОЕ ПОЛЕ - есть ли у пользователя сектор, связанный с этим мероприятием
    private Boolean isMySector;

}