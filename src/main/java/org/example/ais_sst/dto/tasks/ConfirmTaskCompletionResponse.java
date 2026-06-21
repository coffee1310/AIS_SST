package org.example.ais_sst.dto.tasks;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmTaskCompletionResponse {
    private Integer requestId;
    private Integer taskId;
    private String taskTitle;
    private Long studentId;
    private String studentName;
    private String studentSurname;
    private Boolean isCompleted;
    private Instant completedAt;
    private String comment;
}