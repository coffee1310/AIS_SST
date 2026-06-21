package org.example.ais_sst.dto.event_participation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventParticipantFilterDTO {
    private Long eventId;
    private String role;           // Фильтр по роли (организатор, участник, или конкретная роль)
    private String fullName;       // Фильтр по ФИО (частичное совпадение)
    private Integer minPoints;     // Минимальное количество баллов
    private Integer maxPoints;     // Максимальное количество баллов
    private Boolean wasPresent;    // Фильтр по присутствию
    private String entityType;     // Фильтр по типу сущности (ORGANIZER, PARTICIPANT, PARTICIPATION_RECORD)
    private Long currentUserId;    // Текущий пользователь для проверки прав
}