package org.example.ais_sst.dto.task_request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTaskRequestDTO {
    @NotNull(message = "ID задачи обязателен")
    private Long taskId;

    private String comment;
}