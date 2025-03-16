package com.swiftchat.auth_service.service.impl;

import com.swiftchat.auth_service.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender emailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.email.from}")
    private String fromEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @Override
    @Async
    public void sendActivationEmail(String to, String activationKey) {
        try {
            String activationUrl = frontendUrl + "/activate?key=" + activationKey;

            Context context = new Context();
            context.setVariable("activationUrl", activationUrl);

            String htmlContent = templateEngine.process("activation-email", context);
            sendEmail(to, "SwiftChat Account Activation", htmlContent);
            log.info("Sent activation email to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send activation email to {}: {}", to, e.getMessage());
        }
    }

    @Override
    @Async
    public void sendPasswordResetEmail(String to, String resetKey) {
        try {
            String resetUrl = frontendUrl + "/auth/reset-password?token=" + resetKey;

            Context context = new Context();
            context.setVariable("resetUrl", resetUrl);

            String htmlContent = templateEngine.process("password-reset-email", context);
            sendEmail(to, "SwiftChat Password Reset", htmlContent);
            log.info("Sent password reset email to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", to, e.getMessage());
        }
    }

    private void sendEmail(String to, String subject, String htmlContent) throws MessagingException {
        MimeMessage message = emailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromEmail);
        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);

        emailSender.send(message);
    }
}
