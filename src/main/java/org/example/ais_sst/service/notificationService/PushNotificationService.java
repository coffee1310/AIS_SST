package org.example.ais_sst.service.notificationService;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.notifications.NotificationDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Value("${websocket.redis.channel:websocket:notifications}")
    private String notificationChannel;

    public void sendToUser(String userId, String message, String type) {
        NotificationDto notification = new NotificationDto(userId, message, type);
        log.info("Sending notification to user {}: {}", userId, message);

        // 1. Отправляем через WebSocket
        messagingTemplate.convertAndSendToUser(
                userId,
                "/queue/notifications",
                notification
        );

        // 2. Сохраняем в Redis для истории
        saveToHistory(userId, notification);

        // 3. Публикуем в Redis для других инстансов
        try {
            // Используем RedisTemplate для отправки
            redisTemplate.convertAndSend(notificationChannel, notification);
            log.debug("Published to Redis channel: {}", notificationChannel);
        } catch (Exception e) {
            log.error("Failed to publish notification to Redis", e);
        }
    }

    public void sendToAll(String message, String type) {
        NotificationDto notification = new NotificationDto("all", message, type);
        log.info("Sending notification to all: {}", message);

        // 1. Отправляем через WebSocket
        messagingTemplate.convertAndSend("/topic/public", notification);

        // 2. Публикуем в Redis для других инстансов
        try {
            redisTemplate.convertAndSend(notificationChannel, notification);
            log.debug("Published to Redis channel: {}", notificationChannel);
        } catch (Exception e) {
            log.error("Failed to publish notification to Redis", e);
        }
    }

    private void saveToHistory(String userId, NotificationDto notification) {
        try {
            String key = "user:" + userId + ":history";
            redisTemplate.opsForList().leftPush(key, notification);
            redisTemplate.opsForList().trim(key, 0, 99);
            redisTemplate.expire(key, 30, TimeUnit.DAYS);
            log.debug("Saved to history for user: {}", userId);
        } catch (Exception e) {
            log.error("Failed to save to history for user: {}", userId, e);
        }
    }

    @SuppressWarnings("unchecked")
    public List<NotificationDto> getHistory(String userId, int limit) {
        String key = "user:" + userId + ":history";
        List<Object> objects = redisTemplate.opsForList().range(key, 0, limit - 1);

        if (objects == null || objects.isEmpty()) {
            return new ArrayList<>();
        }

        List<NotificationDto> result = new ArrayList<>();
        for (Object obj : objects) {
            try {
                if (obj instanceof NotificationDto) {
                    result.add((NotificationDto) obj);
                } else {
                    String json = objectMapper.writeValueAsString(obj);
                    NotificationDto dto = objectMapper.readValue(json, NotificationDto.class);
                    result.add(dto);
                }
            } catch (Exception e) {
                log.error("Failed to deserialize notification", e);
            }
        }
        return result;
    }
}