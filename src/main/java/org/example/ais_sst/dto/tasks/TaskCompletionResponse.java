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
public class TaskCompletionResponse {
    private Long taskUserId;
    private Long taskId;
    private String taskTitle;
    private Long userId;
    private String userName;
    private String userSurname;
    private Boolean isCompleted;
    private Instant completedAt;
    private Long completedBy;
    private String completedByName;
}