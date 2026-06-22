package org.example.ais_sst.dto.task_request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.ais_sst.entity.enums.TaskRequestStatus;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskRequestFilterDTO {
    private Long id;
    private Long taskId;
    private Long studentId;
    private Long currentUserId;
    private TaskRequestStatus status;
    private Instant filingDateFrom;
    private Instant filingDateTo;
    private Instant reviewedAtFrom;
    private Instant reviewedAtTo;
    private String taskTitle;
    private String taskDescription;
    private Integer taskMaxPeopleCount;
    private Integer taskCountOfPoints;
    private Boolean taskIsCompleted;
    private Boolean taskIsPreassigned;
    private String studentName;
    private String studentSurname;
    private String studentEmail;
    private Boolean myTasks;
    private Boolean myRequests;
    private Boolean pendingOnly;
    private Boolean reviewedOnly;
    private Boolean approvedOnly;
    private Boolean rejectedOnly;
}