package org.example.ais_sst.event.tasks;

import lombok.Getter;

@Getter
public class TaskCompletedEvent {

    private final Long taskId;
    private final String taskTitle;
    private final Long completedByUserId;     // кто отметил задачу выполненной
    private final String completedByEmail;
    private final Long notifyUserId;           // кому отправить уведомление (может быть другой человек)
    private final String message;


    public TaskCompletedEvent(Long taskId,
                              String taskTitle,
                              Long completedByUserId,
                              String completedByEmail,
                              Long notifyUserId,
                              String message) {
        this.taskId = taskId;
        this.taskTitle = taskTitle;
        this.completedByUserId = completedByUserId;
        this.completedByEmail = completedByEmail;
        this.notifyUserId = notifyUserId;
        this.message = message;
    }
}