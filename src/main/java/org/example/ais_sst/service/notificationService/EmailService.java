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


    public void sendEmailVerificationCode(String toEmail, String code, String userName) {
        String subject = "Подтверждение email - AIS SST";

        String textBody = String.format("""
            Здравствуйте, %s!
            
            Вы создаете заявку на регистрацию в системе AIS SST.
            
            Ваш код подтверждения email: %s
            
            Код действителен в течение 15 минут.
            
            Если вы не создавали заявку, проигнорируйте это письмо.
            
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
                    <h2>Подтверждение email</h2>
                </div>
                <div class="content">
                    <p>Здравствуйте, <strong>%s</strong>!</p>
                    <p>Вы создаете заявку на регистрацию в системе AIS SST.</p>
                    <p>Для подтверждения вашего email введите код:</p>
                    <p style="text-align: center;">
                        <span class="code">%s</span>
                    </p>
                    <p>Код действителен в течение <strong>15 минут</strong>.</p>
                    <p>Если вы не создавали заявку, проигнорируйте это письмо.</p>
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
                                                    .data(textBody)
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
            log.info("Verification email sent to {}: {}", toEmail, response.messageId());

        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage());
            throw new RuntimeException("Не удалось отправить email: " + e.getMessage(), e);
        }
    }

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

    /**
     * Отправка уведомления об одобрении заявки на создание аккаунта.
     */
    public void sendAccountApprovedNotification(String toEmail, String fullName, String studentEmail) {
        String subject = "Ваша заявка одобрена — AIS SST";

        String textBody = String.format("""
            Здравствуйте, %s!
            
            Поздравляем! Ваша заявка на создание аккаунта в системе AIS SST была одобрена.
            
            Ваш аккаунт успешно создан.
            Логин (email): %s
            
            Теперь вы можете войти в систему используя свой email и пароль, который вы указали при подаче заявки.
            
            Если у вас возникли вопросы, обратитесь к куратору или председателю.
            
            С уважением,
            Команда AIS SST
            """, fullName != null ? fullName : "пользователь", studentEmail);

        String htmlBody = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { font-family: Arial, sans-serif; color: #333; max-width: 600px; margin: 0 auto; padding: 20px; }
                    .header { background: #28a745; color: white; padding: 20px; border-radius: 8px 8px 0 0; text-align: center; }
                    .content { background: #f8f9fa; padding: 30px; border-radius: 0 0 8px 8px; }
                    .success-icon { font-size: 48px; margin-bottom: 15px; }
                    .login-box { background: white; border: 2px solid #28a745; border-radius: 8px; padding: 20px; margin: 20px 0; text-align: center; }
                    .login-label { font-size: 14px; color: #666; margin-bottom: 5px; }
                    .login-value { font-size: 20px; font-weight: bold; color: #28a745; }
                    .footer { margin-top: 30px; font-size: 13px; color: #888; text-align: center; }
                    .btn { display: inline-block; background: #28a745; color: white; padding: 12px 30px; text-decoration: none; border-radius: 6px; margin-top: 15px; }
                </style>
            </head>
            <body>
                <div class="header">
                    <div class="success-icon">✅</div>
                    <h2>Заявка одобрена!</h2>
                </div>
                <div class="content">
                    <p>Здравствуйте, <strong>%s</strong>!</p>
                    
                    <p>Поздравляем! Ваша заявка на создание аккаунта в системе <strong>AIS SST</strong> была <strong>одобрена</strong>.</p>
                    
                    <p>Ваш аккаунт успешно создан.</p>
                    
                    <div class="login-box">
                        <div class="login-label">Ваш логин для входа:</div>
                        <div class="login-value">%s</div>
                    </div>
                    
                    <p>Теперь вы можете войти в систему, используя email и пароль, указанные при подаче заявки.</p>
                    
                    <p style="margin-top: 25px;">Если у вас возникли вопросы — обращайтесь к куратору или в председателю.</p>
                    
                    <hr style="margin: 30px 0; border: none; border-top: 1px solid #ddd;">
                    
                    <p style="font-size: 14px; color: #666;">С уважением,<br><strong>Команда AIS SST</strong></p>
                </div>
                <div class="footer">
                    © 2026 Автоматизированная информационная система студенческого совета
                </div>
            </body>
            </html>
            """, fullName != null ? fullName : "пользователь", studentEmail);

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
                                                    .data(textBody)
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
            log.info("Account approved notification sent to {}: {}", toEmail, response.messageId());

        } catch (Exception e) {
            log.error("Failed to send account approved email to {}: {}", toEmail, e.getMessage());
            // Не бросаем исключение, чтобы не ломать процесс одобрения заявки
        }
    }

}