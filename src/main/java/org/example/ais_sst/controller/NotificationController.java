package org.example.ais_sst.controller;

import lombok.RequiredArgsConstructor;
import org.example.ais_sst.dto.notifications.NotificationDto;
import org.example.ais_sst.service.notificationService.PushNotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final PushNotificationService notificationService;

    @PostMapping("/user/{userId}")
    public ResponseEntity<String> sendToUser(
            @PathVariable String userId,
            @RequestParam String message,
            @RequestParam(defaultValue = "INFO") String type) {
        notificationService.sendToUser(userId, message, type);
        return ResponseEntity.ok("Notification sent to user: " + userId);
    }

    @PostMapping("/all")
    public ResponseEntity<String> sendToAll(
            @RequestParam String message,
            @RequestParam(defaultValue = "INFO") String type) {
        notificationService.sendToAll(message, type);
        return ResponseEntity.ok("Notification sent to all");
    }

    @GetMapping("/history/{userId}")
    public ResponseEntity<List<NotificationDto>> getHistory(
            @PathVariable String userId,
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(notificationService.getHistory(userId, limit));
    }
}