package org.example.ais_sst.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.notifications.NotificationDto;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisNotificationListener implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            // Получаем JSON строку
            String json = new String(message.getBody());
            log.debug("Received Redis message: {}", json);

            // ДЕСЕРИАЛИЗУЕМ JSON в NotificationDto
            NotificationDto notification = objectMapper.readValue(json, NotificationDto.class);

            log.info("Received Redis notification for user {}: {}",
                    notification.getUserId(), notification.getMessage());

            // Отправляем через WebSocket на этом инстансе
            if ("all".equals(notification.getUserId())) {
                messagingTemplate.convertAndSend("/topic/public", notification);
                log.debug("Broadcasted to /topic/public");
            } else {
                if (notification.getEmail() == null || notification.getEmail().isBlank()) {
                    log.warn("Cannot send personal notification: email is null for userId={}",
                            notification.getUserId());
                    return;
                }

                messagingTemplate.convertAndSendToUser(
                        notification.getEmail(),
                        "/queue/notifications",
                        notification
                );
                log.debug("Sent to user: {}", notification.getUserId());
            }
        } catch (Exception e) {
            log.error("Failed to process Redis notification. Raw message: {}",
                    new String(message.getBody()), e);
        }
    }
}