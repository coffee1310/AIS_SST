package org.example.ais_sst.event.tasks;

import lombok.Getter;

@Getter
public class TaskRequestSubmittedEvent {
    private final Long taskId;
    private final String taskTitle;
    private final Long applicantId;
    private final Long taskCreatorId;

    public TaskRequestSubmittedEvent(Long taskId, String taskTitle, Long applicantId, Long taskCreatorId) {
        this.taskId = taskId;
        this.taskTitle = taskTitle;
        this.applicantId = applicantId;
        this.taskCreatorId = taskCreatorId;
    }
}