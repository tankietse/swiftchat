package com.swiftchat.auth_service.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Email Service Implementation Tests")
class EmailServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private ITemplateEngine templateEngine;

    @Mock
    private MimeMessage mimeMessage;

    @InjectMocks
    private EmailServiceImpl emailService;

    private String fromEmail;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        fromEmail = "noreply@swiftchat.com";
        baseUrl = "https://swiftchat.com";

        // Set properties using reflection
        ReflectionTestUtils.setField(emailService, "fromEmail", fromEmail);
        ReflectionTestUtils.setField(emailService, "baseUrl", baseUrl);

        // Mock MimeMessage creation
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
    }

    @Test
    @DisplayName("Should send activation email with correct parameters")
    void sendActivationEmail_ShouldSendEmailWithCorrectParameters() throws MessagingException {
        // Arrange
        String email = "test@example.com";
        String activationKey = "activation-key-123";
        String expectedProcessedTemplate = "<html>Activation Link Content</html>";

        when(templateEngine.process(eq("email/activation-email"), any(Context.class)))
                .thenReturn(expectedProcessedTemplate);

        // Act
        emailService.sendActivationEmail(email, activationKey);

        // Assert
        // Verify template engine is called with correct template name
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("email/activation-email"), contextCaptor.capture());

        // Verify context variables
        Context capturedContext = contextCaptor.getValue();
        assertEquals(email, capturedContext.getVariable("email"));
        assertEquals(baseUrl + "/auth/activate?key=" + activationKey,
                capturedContext.getVariable("activationUrl"));

        // Verify email is sent
        verify(mailSender).send(eq(mimeMessage));
    }

    @Test
    @DisplayName("Should send password reset email with correct parameters")
    void sendPasswordResetEmail_ShouldSendEmailWithCorrectParameters() throws MessagingException {
        // Arrange
        String email = "test@example.com";
        String resetKey = "reset-key-123";
        String expectedProcessedTemplate = "<html>Password Reset Link Content</html>";

        when(templateEngine.process(eq("email/password-reset-email"), any(Context.class)))
                .thenReturn(expectedProcessedTemplate);

        // Act
        emailService.sendPasswordResetEmail(email, resetKey);

        // Assert
        // Verify template engine is called with correct template name
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("email/password-reset-email"), contextCaptor.capture());

        // Verify context variables
        Context capturedContext = contextCaptor.getValue();
        assertEquals(email, capturedContext.getVariable("email"));
        assertEquals(baseUrl + "/auth/reset-password?key=" + resetKey,
                capturedContext.getVariable("resetUrl"));

        // Verify email is sent
        verify(mailSender).send(eq(mimeMessage));
    }

    @Test
    @DisplayName("Should handle exceptions during activation email sending")
    void sendActivationEmail_WithException_ShouldHandleGracefully() throws MessagingException {
        // Arrange
        String email = "test@example.com";
        String activationKey = "activation-key-123";
        
        when(templateEngine.process(eq("email/activation-email"), any(Context.class)))
                .thenReturn("<html>Activation Link Content</html>");

        // Simulate messaging exception
        doThrow(new MessagingException("Failed to send email"))
                .when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> emailService.sendActivationEmail(email, activationKey));
        
        // Verify the exception message contains useful information
        assertTrue(exception.getMessage().contains("Could not send activation email"));
        assertTrue(exception.getCause() instanceof MessagingException);
    }

    @Test
    @DisplayName("Should handle exceptions during password reset email sending")
    void sendPasswordResetEmail_WithException_ShouldHandleGracefully() throws MessagingException {
        // Arrange
        String email = "test@example.com";
        String resetKey = "reset-key-123";
        
        when(templateEngine.process(eq("email/password-reset-email"), any(Context.class)))
                .thenReturn("<html>Password Reset Link Content</html>");

        // Simulate messaging exception
        doThrow(new MessagingException("Failed to send email"))
                .when(mailSender).send(any(MimeMessage.class));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> emailService.sendPasswordResetEmail(email, resetKey));
        
        // Verify the exception message contains useful information
        assertTrue(exception.getMessage().contains("Could not send password reset email"));
        assertTrue(exception.getCause() instanceof MessagingException);
    }
}
