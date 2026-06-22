package org.example.ais_sst.event.tasks;

import lombok.Getter;

@Getter
public class TaskAssignedEvent {
    private final Long taskId;
    private final String taskTitle;
    private final Long assignedUserId;

    public TaskAssignedEvent(Long taskId, String taskTitle, Long assignedUserId) {
        this.taskId = taskId;
        this.taskTitle = taskTitle;
        this.assignedUserId = assignedUserId;
    }
}