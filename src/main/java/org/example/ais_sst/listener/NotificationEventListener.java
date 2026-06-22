package org.example.ais_sst.listener;

import lombok.RequiredArgsConstructor;
import org.example.ais_sst.event.UserAccountCreatingRequestEvent;
import org.example.ais_sst.service.notificationService.EmailService;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationEventListener {

    private final EmailService notificationService;

    @EventListener
    public void onUserAccountCreatingRequest(UserAccountCreatingRequestEvent event) {

    }
}
