package com.swiftchat.auth_service.service;

/**
 * Service for sending emails.
 */
public interface EmailService {

    /**
     * Send an account activation email.
     *
     * @param to            the recipient email
     * @param activationKey the activation key to include in the email
     */
    void sendActivationEmail(String to, String activationKey);

    /**
     * Send a password reset email.
     *
     * @param to       the recipient email
     * @param resetKey the reset key to include in the email
     */
    void sendPasswordResetEmail(String to, String resetKey);
}
