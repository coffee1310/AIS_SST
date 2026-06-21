package org.example.ais_sst.dto.tasks;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskFilterDTO {
    private Integer id;
    private String title;
    private String description;
    private Long creatorId;
    private Long currentUserId;

    // Фильтры по датам
    private Instant deadlineFrom;
    private Instant deadlineTo;

    // Фильтры по полям
    private Integer maxPeopleCount;
    private Integer countOfPoints;
    private Boolean isCompleted;
    private Boolean isDeleted;
    private Boolean isPreassigned;

    // Новые фильтры
    private Boolean createdByMe;    // Задачи созданные мной
    private Boolean assignedToMe;   // Задачи назначенные мне (которые я выполняю)
}