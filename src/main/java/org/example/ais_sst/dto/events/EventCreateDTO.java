package org.example.ais_sst.dto.events;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class EventCreateDTO {

    @NotBlank(message = "Название мероприятия не может быть пустым")
    @Size(max = 255, message = "Название не должно превышать 255 символов")
    private String title;

    private String description;

    private String photo;  // Base64 строка фото

    private LocalDate dateOfEvent;

    private LocalTime startTime;

    private LocalTime endTime;

    private String venue;

    private String referenceToPosition;

    private Boolean isPublic = true;

    private Boolean isDraft = true;

    private List<Long> organizerIds;
}