package org.example.ais_sst.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.service.notificationService.EmailNotificationService;
import org.example.ais_sst.service.notificationService.SubscriberManagementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailNotificationService emailService;
    private final SubscriberManagementService subscriberService;

    // Отправка приветственного письма
    @PostMapping("/welcome")
    public ResponseEntity<String> sendWelcomeEmail(
            @RequestParam String email,
            @RequestParam String name) {
        try {
            emailService.sendWelcomeEmail(email, name);
            return ResponseEntity.ok("Welcome email sent successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    // Добавление подписчика
    @PostMapping("/subscribe")
    public ResponseEntity<String> addSubscriber(
            @RequestParam String listId,
            @RequestParam String email,
            @RequestParam(required = false) String name) {
        try {
            subscriberService.addSubscriberToList(listId, email, name);
            return ResponseEntity.ok("Subscriber added successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed: " + e.getMessage());
        }
    }

    // Получение всех кампаний
    @GetMapping("/campaigns")
    public ResponseEntity<List<Map<String, Object>>> getCampaigns() {
        try {
            List<Map<String, Object>> campaigns = emailService.getAllCampaigns();
            return ResponseEntity.ok(campaigns);
        } catch (Exception e) {
            log.error("Error getting campaigns", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}