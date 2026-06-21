package org.example.ais_sst.dto.tasks;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaskRequest {
    @NotBlank(message = "Название задачи обязательно")
    private String title;

    private String description;

    private Instant deadline;

    @Min(value = 0, message = "Максимальное количество людей должно быть >= 0")
    @Builder.Default
    private Integer maxPeopleCount = 0;

    @Min(value = 1, message = "Количество баллов должно быть >= 1")
    @Builder.Default
    private Integer countOfPoints = 1;

    @Builder.Default
    private Boolean isPreassigned = false;

    // Список пользователей, если isPreassigned = true
    private List<Long> userIds;
}