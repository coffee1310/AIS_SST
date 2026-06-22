package org.example.ais_sst.service.notificationService;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.notifications.NotificationDto;
import org.example.ais_sst.entity.User;
import org.example.ais_sst.repository.UserRepository;
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
    private final UserRepository userRepository;

    @Value("${websocket.redis.channel:websocket:notifications}")
    private String notificationChannel;

    public void sendToUser(String userId, String message, String type) {
        NotificationDto notification = new NotificationDto(userId, message, type);

        // Получаем email пользователя (если ещё не делаешь)
        // String email = userRepository.findById(...).map(User::getStudentEmail).orElse(null);
        // notification.setEmail(email);

        log.info("Sending notification to user {}: {}", userId, message);

        saveToHistory(userId, notification);

        // Только публикация в Redis
        try {
            redisTemplate.convertAndSend(notificationChannel, notification);
            log.debug("Published to Redis channel: {}", notificationChannel);
        } catch (Exception e) {
            log.error("Failed to publish notification to Redis", e);
        }
    }

    public void sendToAll(String message, String type) {
        NotificationDto notification = new NotificationDto("all", message, type);
        log.info("Sending notification to all: {}", message);

        // Только публикация в Redis (без прямого convertAndSend)
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