package org.example.ais_sst.dto.events;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * DTO для отчета по мероприятиям.
 * Содержит агрегированную статистику по каждому мероприятию.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventReportDTO {

    private Long eventId;
    private String title;

    private Boolean isCompleted;
    private String completionStatus;      // "Завершено" / "Не завершено"

    private Boolean isPublic;
    private String visibilityStatus;      // "Публичное" / "Не публичное"

    private Boolean isFreeEvent;
    private String freeStatus;            // "Свободное" / "Платное"

    private LocalDate eventDate;

    private Integer totalPeopleCount;     // Общее кол-во человек (организаторы + участники + исполнители)
    private Integer organizersCount;      // Кол-во организаторов
    private Integer performersCount;      // Кол-во исполнителей (участие через роли/сектора)
    private Integer participantsCount;    // Кол-во участников (EventParticipant)

    private Integer maxParticipantsCount; // Максимальное кол-во участников (из события)
    private Integer maxOrganizersCount;   // Максимальное кол-во организаторов
}