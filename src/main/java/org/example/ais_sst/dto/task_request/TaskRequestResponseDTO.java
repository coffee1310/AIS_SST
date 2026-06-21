package org.example.ais_sst.dto.task_request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequestResponseDTO {
    private Integer id;
    private Integer taskId;
    private String taskTitle;
    private Long studentId;
    private String studentName;
    private String studentSurname;
    private String status;
    private Instant filingDate;
    private Instant reviewedAt;
}