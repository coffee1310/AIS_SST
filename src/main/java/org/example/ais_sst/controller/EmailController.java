package org.example.ais_sst.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ais_sst.dto.email.EmailRequest;
import org.example.ais_sst.service.notificationService.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/email")
@RequiredArgsConstructor
public class EmailController {

    private final EmailService emailService;

    @PostMapping("/send")
    public ResponseEntity<String> sendEmail(@RequestBody EmailRequest request) {
        String result = emailService.sendSimpleEmail(
                request.getTo(),
                request.getSubject(),
                request.getBody()
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/send-html")
    public ResponseEntity<String> sendHtmlEmail(@RequestBody EmailRequest request) {
        String result = emailService.sendHtmlEmail(
                request.getTo(),
                request.getSubject(),
                request.getBody()
        );
        return ResponseEntity.ok(result);
    }
}