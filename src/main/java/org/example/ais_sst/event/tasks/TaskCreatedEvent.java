package org.example.ais_sst.event.tasks;

import lombok.Getter;

@Getter
public class TaskCreatedEvent {
    private final Long taskId;
    private final String taskTitle;
    private final Long creatorId;
    private final Long sectorId;           // важно для рассылки участникам сектора

    public TaskCreatedEvent(Long taskId, String taskTitle, Long creatorId, Long sectorId) {
        this.taskId = taskId;
        this.taskTitle = taskTitle;
        this.creatorId = creatorId;
        this.sectorId = sectorId;
    }
}