package org.example.ais_sst.service.notificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final SesV2Client sesClient;

    @Value("${yandex.postbox.from-email}")
    private String fromEmail;

    // Простое текстовое письмо
    public String sendSimpleEmail(String to, String subject, String body) {
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .fromEmailAddress(fromEmail)
                    .destination(Destination.builder().toAddresses(to).build())
                    .content(EmailContent.builder()
                            .simple(Message.builder() // Используем Message вместо SimpleEmailContent
                                    .subject(Content.builder().data(subject).build())
                                    .body(Body.builder()
                                            .text(Content.builder().data(body).build())
                                            .build())
                                    .build())
                            .build())
                    .build();

            SendEmailResponse response = sesClient.sendEmail(request);
            return "✅ Письмо отправлено! MessageId: " + response.messageId();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("❌ Ошибка отправки: " + e.getMessage(), e);
        }
    }

    // HTML письмо
    public String sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .fromEmailAddress(fromEmail)
                    .destination(Destination.builder().toAddresses(to).build())
                    .content(EmailContent.builder()
                            .simple(Message.builder() // Используем Message вместо SimpleEmailContent
                                    .subject(Content.builder().data(subject).build())
                                    .body(Body.builder()
                                            .html(Content.builder().data(htmlBody).build())
                                            .build())
                                    .build())
                            .build())
                    .build();

            SendEmailResponse response = sesClient.sendEmail(request);
            return "✅ HTML письмо отправлено! MessageId: " + response.messageId();

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("❌ Ошибка отправки HTML: " + e.getMessage(), e);
        }
    }

    public void sendPasswordResetCode(String toEmail, String code, String userName) {
        String subject = "Восстановление пароля - AIS SST";

        String body = String.format("""
            Здравствуйте, %s!
            
            Вы запросили восстановление пароля для вашей учетной записи AIS SST.
            
            Ваш код для сброса пароля: %s
            
            Код действителен в течение 24 часов.
            
            Если вы не запрашивали восстановление пароля, проигнорируйте это письмо.
            
            С уважением,
            Команда AIS SST
            """, userName != null ? userName : "пользователь", code);

        String htmlBody = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #4A90D9; color: white; padding: 15px; border-radius: 5px 5px 0 0; }
                    .content { background: #f9f9f9; padding: 20px; border-radius: 0 0 5px 5px; }
                    .code { font-size: 32px; font-weight: bold; color: #4A90D9; background: white; padding: 10px 20px; display: inline-block; border-radius: 5px; letter-spacing: 5px; border: 1px solid #ddd; }
                    .footer { margin-top: 20px; font-size: 12px; color: #999; text-align: center; }
                </style>
            </head>
            <body>
                <div class="header">
                    <h2>Восстановление пароля</h2>
                </div>
                <div class="content">
                    <p>Здравствуйте, <strong>%s</strong>!</p>
                    <p>Вы запросили восстановление пароля для вашей учетной записи AIS SST.</p>
                    <p>Ваш код для сброса пароля:</p>
                    <p style="text-align: center;">
                        <span class="code">%s</span>
                    </p>
                    <p>Код действителен в течение <strong>24 часов</strong>.</p>
                    <p>Если вы не запрашивали восстановление пароля, проигнорируйте это письмо.</p>
                    <hr>
                    <p style="font-size: 14px; color: #666;">С уважением,<br>Команда AIS SST</p>
                </div>
                <div class="footer">
                    © 2026 AIS SST. Все права защищены.
                </div>
            </body>
            </html>
            """, userName != null ? userName : "пользователь", code);

        try {
            SendEmailRequest request = SendEmailRequest.builder()
                    .fromEmailAddress(fromEmail)
                    .destination(Destination.builder()
                            .toAddresses(toEmail)
                            .build())
                    .content(EmailContent.builder()
                            .simple(Message.builder()
                                    .subject(Content.builder()
                                            .data(subject)
                                            .charset("UTF-8")
                                            .build())
                                    .body(Body.builder()
                                            .text(Content.builder()
                                                    .data(body)
                                                    .charset("UTF-8")
                                                    .build())
                                            .html(Content.builder()
                                                    .data(htmlBody)
                                                    .charset("UTF-8")
                                                    .build())
                                            .build())
                                    .build())
                            .build())
                    .build();

            SendEmailResponse response = sesClient.sendEmail(request);
            log.info("Password reset email sent to {}: {}", toEmail, response.messageId());

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Не удалось отправить email: " + e.getMessage(), e);
        }
    }
}