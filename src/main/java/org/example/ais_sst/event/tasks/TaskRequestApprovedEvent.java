package org.example.ais_sst.event.tasks;

import lombok.Getter;

@Getter
public class TaskRequestApprovedEvent {
    private final Long taskId;
    private final String taskTitle;
    private final Long applicantId;

    public TaskRequestApprovedEvent(Long taskId, String taskTitle, Long applicantId) {
        this.taskId = taskId;
        this.taskTitle = taskTitle;
        this.applicantId = applicantId;
    }
}