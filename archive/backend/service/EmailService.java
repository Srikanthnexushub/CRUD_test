package org.example.service;

import org.example.entity.EmailNotification;
import org.example.entity.User;

import java.util.Map;

public interface EmailService {

    /**
     * Queue an email for asynchronous sending
     */
    void queueEmail(User user, String templateName, Map<String, Object> variables);

    /**
     * Queue an email with specific priority
     */
    void queueEmail(User user, String templateName, Map<String, Object> variables, int priority);

    /**
     * Send email synchronously (blocks until sent)
     */
    void sendEmailSync(String recipientEmail, String subject, String htmlContent);

    /**
     * Process pending emails from queue (called by scheduler)
     */
    void processPendingEmails();

    /**
     * Retry failed emails with exponential backoff
     */
    void retryFailedEmails();

    /**
     * Send test email to verify SMTP configuration
     */
    void sendTestEmail(String recipientEmail);

    /**
     * Send daily digest to users
     */
    void sendDailyDigest();

    /**
     * Send weekly digest to users
     */
    void sendWeeklyDigest();
}
