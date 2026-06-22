package org.example.ais_sst.event.tasks;

import lombok.Getter;

@Getter
public class TaskRequestRejectedEvent {
    private final Long taskId;
    private final String taskTitle;
    private final Long applicantId;

    public TaskRequestRejectedEvent(Long taskId, String taskTitle, Long applicantId) {
        this.taskId = taskId;
        this.taskTitle = taskTitle;
        this.applicantId = applicantId;
    }
}