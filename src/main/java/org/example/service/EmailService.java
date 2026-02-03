package org.example.service;

import org.example.dto.EmailRequest;

import java.util.Map;

/**
 * Email Service Interface
 * Provides email notification functionality with template support
 */
public interface EmailService {

    /**
     * Send a simple text email
     */
    void sendSimpleEmail(String to, String subject, String text);

    /**
     * Send an HTML email using a template
     */
    void sendTemplateEmail(String to, String subject, String templateName, Map<String, Object> variables);

    /**
     * Send welcome email to new user
     */
    void sendWelcomeEmail(String to, String username);

    /**
     * Send password reset email
     */
    void sendPasswordResetEmail(String to, String username, String resetToken);

    /**
     * Send account locked notification
     */
    void sendAccountLockedEmail(String to, String username, String reason);

    /**
     * Send account unlocked notification
     */
    void sendAccountUnlockedEmail(String to, String username);

    /**
     * Send MFA enabled notification
     */
    void sendMFAEnabledEmail(String to, String username);

    /**
     * Send MFA disabled notification
     */
    void sendMFADisabledEmail(String to, String username);

    /**
     * Send password changed notification
     */
    void sendPasswordChangedEmail(String to, String username);

    /**
     * Send suspicious login activity alert
     */
    void sendSuspiciousLoginEmail(String to, String username, String ipAddress, String location, String device);

    /**
     * Send new login from unknown device notification
     */
    void sendNewDeviceLoginEmail(String to, String username, String device, String ipAddress, String location);

    /**
     * Send generic notification email
     */
    void sendNotificationEmail(EmailRequest request);

    /**
     * Test email configuration
     */
    boolean testEmailConfiguration(String testRecipient);
}
