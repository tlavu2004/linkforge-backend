package com.tlavu.linkforge.infrastructure.service;

import com.tlavu.linkforge.shared.service.MessageService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class EmailService {

    private final JavaMailSender mailSender;
    private final MessageService messageService;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Async
    public void sendVerificationEmail(String to, String otp, Locale locale) {
        String subject = messageService.getMessage("mail.verify.subject", locale);
        String body = buildOtpEmailBody(
                messageService.getMessage("mail.verify.title", locale),
                otp,
                messageService.getMessage("mail.verify.description", locale),
                messageService.getMessage("mail.verify.footer", locale));
        sendHtmlEmail(to, subject, body);
    }

    @Async
    public void sendPasswordResetEmail(String to, String otp, Locale locale) {
        String subject = messageService.getMessage("mail.reset.subject", locale);
        String body = buildOtpEmailBody(
                messageService.getMessage("mail.reset.title", locale),
                otp,
                messageService.getMessage("mail.reset.description", locale),
                messageService.getMessage("mail.reset.footer", locale));
        sendHtmlEmail(to, subject, body);
    }

    private void sendHtmlEmail(@NonNull String to, @NonNull String subject, @NonNull String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlBody, true);
            mailSender.send(message);
            log.info("Email sent to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage(), e);
        }
    }

    private String buildOtpEmailBody(String title, String otp, String description, String footer) {
        return """
                <div style="font-family: 'Segoe UI', Arial, sans-serif; max-width: 480px; margin: 0 auto; padding: 32px; background: #ffffff; border-radius: 16px; border: 1px solid #e5e7eb;">
                  <div style="text-align: center; margin-bottom: 24px;">
                    <h1 style="font-size: 20px; font-weight: 700; color: #111827; margin: 0;">🔗 LinkForge</h1>
                  </div>
                  <h2 style="font-size: 18px; font-weight: 600; color: #111827; text-align: center; margin-bottom: 8px;">%s</h2>
                  <p style="font-size: 14px; color: #6b7280; text-align: center; margin-bottom: 24px;">%s</p>
                  <div style="background: #f3f4f6; border-radius: 12px; padding: 24px; text-align: center; margin-bottom: 24px;">
                    <span style="font-size: 36px; font-weight: 700; letter-spacing: 8px; color: #4f46e5;">%s</span>
                  </div>
                  <p style="font-size: 12px; color: #9ca3af; text-align: center;">%s</p>
                </div>
                """
                .formatted(title, description, otp, footer);
    }
}
