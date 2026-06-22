package org.example.ais_sst.event.tasks;

import lombok.Getter;

@Getter
public class TaskCompletedEvent {
    private final Long taskId;
    private final String taskTitle;
    private final Long completedByUserId;

    public TaskCompletedEvent(Long taskId, String taskTitle, Long completedByUserId) {
        this.taskId = taskId;
        this.taskTitle = taskTitle;
        this.completedByUserId = completedByUserId;
    }
}