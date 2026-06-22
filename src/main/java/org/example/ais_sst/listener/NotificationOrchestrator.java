package org.example.ais_sst.listener;

import lombok.RequiredArgsConstructor;
import org.example.ais_sst.event.tasks.TaskCompletedEvent;
import org.example.ais_sst.service.notificationService.EmailService;
import org.example.ais_sst.service.notificationService.PushNotificationService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class NotificationOrchestrator {

    private final EmailService emailService;
    private final PushNotificationService inAppService;

//    @EventListener
//    public void onEventCreated(EventCreatedEvent event) {
//        // Логика принятия решения
//        inAppService.sendToAll(...);
//        emailService.sendToAdmins(...);
//    }
//
//    @EventListener
//    public void onPointsAwarded(PointsAwardedEvent event) {
//        inAppService.sendToUser(event.getUserId(), ...);
//        // emailService.send(...) — если нужно
//    }

    @EventListener
    @TransactionalEventListener
    public void onTaskCompleted(TaskCompletedEvent event) {
        inAppService.sendToUser(
                "user" + event.getNotifyUserId(),           // ← правильный формат
                event.getMessage(),
                "INFO"
        );
    }
}
