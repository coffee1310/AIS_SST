package org.example.ais_sst.dto.events;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class EventCreateDTO {

    @NotBlank(message = "Название мероприятия не может быть пустым")
    @Size(max = 255, message = "Название не должно превышать 255 символов")
    private String title;

    private String description;

    private String photo;  // Путь к фото

    @NotNull(message = "Время начала обязательно")
    private LocalDateTime startTime;

    @NotNull(message = "Время окончания обязательно")
    private LocalDateTime endTime;

    private String location;

    @NotNull(message = "Должен быть хотя бы один организатор")
    @Size(min = 1, message = "Должен быть хотя бы один организатор")
    private List<Long> organizerIds;  // ID пользователей-организаторов
}