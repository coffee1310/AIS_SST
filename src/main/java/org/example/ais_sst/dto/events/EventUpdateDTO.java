package org.example.ais_sst.dto.events;

import jakarta.validation.constraints.Size;
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
public class EventUpdateDTO {

    @Size(max = 255, message = "Название не должно превышать 255 символов")
    private String title;

    private String description;

    private String photo;

    private LocalDate dateOfEvent;

    private String referenceToPosition;

    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private String venue;  // Изменено с location на venue

    private Boolean isActive;

    private Boolean isPublic;  // Добавлено поле isPublic

    private List<Long> organizerIds;  // Обновленный список организаторов
}