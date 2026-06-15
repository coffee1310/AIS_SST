package org.example.ais_sst.dto.events;

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
public class EventUpdateDTO {

    @Size(max = 255, message = "Название не должно превышать 255 символов")
    private String title;

    private String description;

    private String photo;  // Base64 строка фото

    private LocalDate dateOfEvent;

    private LocalTime startTime;

    private LocalTime endTime;

    private String venue;

    private String referenceToPosition;

    private Boolean isPublic;

    private Boolean isDraft;

    private Boolean isActive;

    private List<Long> organizerIds;

    private Boolean isFreeEvent = true;  // true - можно участвовать, false - нельзя участвовать

    private Integer maxParticipantsCount = 0;

    private Integer maxOrganizersCount = 0;

    private Long sectorId;
}