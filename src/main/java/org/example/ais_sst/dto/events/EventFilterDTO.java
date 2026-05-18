package org.example.ais_sst.dto.events;

import lombok.Builder;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class EventFilterDTO {
    private Long id;
    private String title;
    private String venue;
    private String description;
    private String referenceToPosition;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dateTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime startTimeFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime startTimeTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime endTimeFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime endTimeTo;

    private Boolean isPublic;
    private Boolean isDraft;
    private Boolean isCompleted;
    private Boolean isActive;
    private Long creatorId;

    private Boolean isResponsibleSector;
    private Long currentUserId;

    private Boolean isOrganizer;
}