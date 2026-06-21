package org.example.ais_sst.dto.tasks;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTaskCompletionRequest {
    @NotNull(message = "Статус выполнения обязателен")
    private Boolean isCompleted;
}
