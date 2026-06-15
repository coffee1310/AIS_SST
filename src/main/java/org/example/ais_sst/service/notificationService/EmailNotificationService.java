package org.example.ais_sst.service.notificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailNotificationService {

    private final DashaMailClient dashaMailClient;

    // Отправка простого письма
    public void sendWelcomeEmail(String to, String name) {
        String subject = "Добро пожаловать!";
        String htmlContent = String.format("""
            <h1>Здравствуйте, %s!</h1>
            <p>Рады приветствовать вас в нашем сервисе.</p>
            <p>С уважением,<br>Команда поддержки</p>
            """, name);

        try {
            dashaMailClient.sendEmail(to, subject, htmlContent);
            log.info("Welcome email sent to: {}", to);
        } catch (IOException e) {
            log.error("Failed to send welcome email to: {}", to, e);
            throw new RuntimeException("Email sending failed", e);
        }
    }

    // Отправка письма с подтверждением
    public void sendVerificationEmail(String to, String verificationCode) {
        String subject = "Подтверждение email";
        String htmlContent = String.format("""
            <h1>Подтверждение регистрации</h1>
            <p>Ваш код подтверждения: <strong>%s</strong></p>
            <p>Введите этот код для завершения регистрации.</p>
            """, verificationCode);

        try {
            dashaMailClient.sendEmail(to, subject, htmlContent);
            log.info("Verification email sent to: {}", to);
        } catch (IOException e) {
            log.error("Failed to send verification email", e);
            throw new RuntimeException("Verification email failed", e);
        }
    }

    // Получение списка всех кампаний
    public List<Map<String, Object>> getAllCampaigns() {
        try {
            return dashaMailClient.getCampaigns();
        } catch (IOException e) {
            log.error("Failed to get campaigns", e);
            return List.of(); // возвращаем пустой список в случае ошибки
        }
    }

    // Вывод статистики кампаний в консоль
    public void printCampaignStats() {
        try {
            List<Map<String, Object>> campaigns = dashaMailClient.getCampaigns();
            for (Map<String, Object> campaign : campaigns) {
                log.info("Campaign: {}, Status: {}, Sent: {}",
                        campaign.get("name"),
                        campaign.get("status"),
                        campaign.get("sent_count"));
            }
        } catch (IOException e) {
            log.error("Failed to get campaign stats", e);
        }
    }
}