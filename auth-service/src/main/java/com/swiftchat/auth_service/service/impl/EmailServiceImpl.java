package com.swiftchat.auth_service.service.impl;

import com.swiftchat.auth_service.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final ITemplateEngine templateEngine;

    @Value("${app.email.from:no-reply@swiftchat.com}")
    private String fromEmail;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Override
    public void sendActivationEmail(String to, String activationKey) {
        try {
            // Create a context with variables for the template
            Context context = new Context();
            context.setVariable("email", to);
            context.setVariable("activationUrl", frontendUrl + "/auth/activate?key=" + activationKey);

            // Update the template path - remove 'email/' prefix
            String emailContent = templateEngine.process("activation-email", context);

            // Create and send the email
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Activate your SwiftChat account");
            helper.setText(emailContent, true);

            mailSender.send(mimeMessage);
            log.info("Activation email sent to {}", to);
        } catch (MessagingException e) {
            log.error("Could not send activation email to {}", to, e);
            throw new RuntimeException("Could not send activation email: " + e.getMessage(), e);
        }
    }

    @Override
    public void sendPasswordResetEmail(String to, String resetKey) {
        try {
            // Create a context with variables for the template
            Context context = new Context();
            context.setVariable("email", to);
            context.setVariable("resetUrl", frontendUrl + "/auth/reset-password?key=" + resetKey);

            // Update the template path - remove 'email/' prefix
            String emailContent = templateEngine.process("password-reset-email", context);

            // Create and send the email
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("Reset your SwiftChat password");
            helper.setText(emailContent, true);

            mailSender.send(mimeMessage);
            log.info("Password reset email sent to {}", to);
        } catch (MessagingException e) {
            log.error("Could not send password reset email to {}", to, e);
            throw new RuntimeException("Could not send password reset email: " + e.getMessage(), e);
        }
    }
}
