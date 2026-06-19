package org.example.ais_sst.service.notificationService;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.*;


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
}