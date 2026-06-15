package org.example.ais_sst.service.notificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriberManagementService {

    private final DashaMailClient dashaMailClient;

    // Добавление подписчика
    public void addSubscriberToList(String listId, String email, String name) {
        try {
            dashaMailClient.addSubscriber(listId, email, name);
            log.info("Subscriber {} added to list {}", email, listId);
        } catch (IOException e) {
            log.error("Failed to add subscriber: {}", email, e);
            throw new RuntimeException("Failed to add subscriber", e);
        }
    }

    // Удаление подписчика
    public void removeSubscriber(String listId, String email) {
        try {
            dashaMailClient.deleteSubscriber(listId, email);
            log.info("Subscriber {} removed from list {}", email, listId);
        } catch (IOException e) {
            log.error("Failed to remove subscriber: {}", email, e);
            throw new RuntimeException("Failed to remove subscriber", e);
        }
    }

    // Массовое добавление подписчиков
    public void addMultipleSubscribers(String listId, List<SubscriberInfo> subscribers) {
        int successCount = 0;
        int failCount = 0;

        for (SubscriberInfo subscriber : subscribers) {
            try {
                dashaMailClient.addSubscriber(listId, subscriber.getEmail(), subscriber.getName());
                successCount++;
            } catch (IOException e) {
                failCount++;
                log.error("Failed to add subscriber: {}", subscriber.getEmail(), e);
            }
        }

        log.info("Bulk add completed. Success: {}, Failed: {}", successCount, failCount);
    }

    // Вспомогательный класс
    public static class SubscriberInfo {
        private String email;
        private String name;

        public SubscriberInfo(String email, String name) {
            this.email = email;
            this.name = name;
        }

        public String getEmail() { return email; }
        public String getName() { return name; }
    }
}