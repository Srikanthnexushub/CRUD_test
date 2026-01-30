package org.example.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.EmailNotification;
import org.example.entity.NotificationPreference;
import org.example.entity.User;
import org.example.repository.EmailNotificationRepository;
import org.example.repository.NotificationPreferenceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final EmailNotificationRepository emailNotificationRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;

    @Value("${email.from.address}")
    private String fromAddress;

    @Value("${email.from.name}")
    private String fromName;

    @Value("${email.max.retries}")
    private int maxRetries;

    @Value("${email.batch.size}")
    private int batchSize;

    @Value("${email.enabled}")
    private boolean emailEnabled;

    @Override
    @Transactional
    public void queueEmail(User user, String templateName, Map<String, Object> variables) {
        queueEmail(user, templateName, variables, 5);
    }

    @Override
    @Transactional
    public void queueEmail(User user, String templateName, Map<String, Object> variables, int priority) {
        if (!emailEnabled) {
            log.info("Email sending is disabled. Skipping email queue for user: {}", user.getUsername());
            return;
        }

        // Check user notification preferences
        NotificationPreference preference = notificationPreferenceRepository.findByUser(user)
                .orElse(createDefaultPreference(user));

        if (!preference.getEmailEnabled()) {
            log.info("Email notifications disabled for user: {}", user.getUsername());
            return;
        }

        // Check specific event preferences
        if (!shouldSendEmail(templateName, preference)) {
            log.info("Email template {} disabled for user: {}", templateName, user.getUsername());
            return;
        }

        EmailNotification notification = new EmailNotification();
        notification.setUser(user);
        notification.setRecipientEmail(user.getEmail());
        notification.setTemplateName(templateName);
        notification.setTemplateVariables(variables);
        notification.setPriority(priority);
        notification.setStatus("PENDING");
        notification.setRetryCount(0);
        notification.setMaxRetries(maxRetries);

        // Generate subject from template name
        notification.setSubject(generateSubject(templateName));

        emailNotificationRepository.save(notification);
        log.info("Email queued for user: {} with template: {}", user.getUsername(), templateName);
    }

    @Override
    public void sendEmailSync(String recipientEmail, String subject, String htmlContent) {
        if (!emailEnabled) {
            log.info("Email sending is disabled. Skipping email send to: {}", recipientEmail);
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress, fromName);
            helper.setTo(recipientEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent successfully to: {}", recipientEmail);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", recipientEmail, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    @Async
    @Scheduled(fixedDelayString = "${email.retry.delay.minutes:5}", timeUnit = java.util.concurrent.TimeUnit.MINUTES)
    public void processPendingEmails() {
        if (!emailEnabled) {
            return;
        }

        List<EmailNotification> pendingEmails = emailNotificationRepository
                .findPendingEmails(LocalDateTime.now(), PageRequest.of(0, batchSize));

        if (pendingEmails.isEmpty()) {
            return;
        }

        log.info("Processing {} pending emails", pendingEmails.size());

        for (EmailNotification notification : pendingEmails) {
            try {
                sendEmail(notification);
                notification.setStatus("SENT");
                notification.setSentAt(LocalDateTime.now());
                emailNotificationRepository.save(notification);
                log.info("Email sent successfully: ID {}", notification.getId());
            } catch (Exception e) {
                notification.setRetryCount(notification.getRetryCount() + 1);
                if (notification.getRetryCount() >= notification.getMaxRetries()) {
                    notification.setStatus("FAILED");
                    log.error("Email failed after {} retries: ID {}", notification.getMaxRetries(), notification.getId());
                }
                notification.setErrorMessage(e.getMessage());
                emailNotificationRepository.save(notification);
                log.error("Failed to send email: ID {}", notification.getId(), e);
            }
        }
    }

    @Override
    @Async
    @Scheduled(cron = "0 */10 * * * *") // Every 10 minutes
    public void retryFailedEmails() {
        if (!emailEnabled) {
            return;
        }

        List<EmailNotification> failedEmails = emailNotificationRepository
                .findFailedEmailsForRetry(PageRequest.of(0, batchSize));

        if (failedEmails.isEmpty()) {
            return;
        }

        log.info("Retrying {} failed emails", failedEmails.size());

        for (EmailNotification notification : failedEmails) {
            try {
                // Exponential backoff delay
                long delayMinutes = (long) Math.pow(2, notification.getRetryCount()) * 5;
                LocalDateTime eligibleTime = notification.getUpdatedAt().plusMinutes(delayMinutes);

                if (LocalDateTime.now().isBefore(eligibleTime)) {
                    continue; // Not yet time to retry
                }

                sendEmail(notification);
                notification.setStatus("SENT");
                notification.setSentAt(LocalDateTime.now());
                emailNotificationRepository.save(notification);
                log.info("Email retry successful: ID {}", notification.getId());
            } catch (Exception e) {
                notification.setRetryCount(notification.getRetryCount() + 1);
                if (notification.getRetryCount() >= notification.getMaxRetries()) {
                    notification.setStatus("FAILED");
                }
                notification.setErrorMessage(e.getMessage());
                emailNotificationRepository.save(notification);
                log.error("Email retry failed: ID {}", notification.getId(), e);
            }
        }
    }

    @Override
    public void sendTestEmail(String recipientEmail) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("testMessage", "This is a test email from CRUD Test application.");
        variables.put("timestamp", LocalDateTime.now().toString());

        Context context = new Context();
        context.setVariables(variables);

        String htmlContent = templateEngine.process("email/test-email", context);
        sendEmailSync(recipientEmail, "Test Email from CRUD Test", htmlContent);
    }

    @Override
    @Scheduled(cron = "0 0 8 * * *") // Daily at 8 AM
    public void sendDailyDigest() {
        List<NotificationPreference> preferences = notificationPreferenceRepository
                .findAllWithDailyDigestEnabled();

        log.info("Sending daily digest to {} users", preferences.size());

        for (NotificationPreference pref : preferences) {
            try {
                Map<String, Object> variables = new HashMap<>();
                variables.put("user", pref.getUser());
                variables.put("date", LocalDateTime.now());
                // Add digest data here

                queueEmail(pref.getUser(), "email/daily-digest", variables, 3);
            } catch (Exception e) {
                log.error("Failed to queue daily digest for user: {}", pref.getUser().getUsername(), e);
            }
        }
    }

    @Override
    @Scheduled(cron = "0 0 8 * * MON") // Weekly on Monday at 8 AM
    public void sendWeeklyDigest() {
        List<NotificationPreference> preferences = notificationPreferenceRepository
                .findAllWithWeeklyDigestEnabled();

        log.info("Sending weekly digest to {} users", preferences.size());

        for (NotificationPreference pref : preferences) {
            try {
                Map<String, Object> variables = new HashMap<>();
                variables.put("user", pref.getUser());
                variables.put("weekStart", LocalDateTime.now().minusWeeks(1));
                variables.put("weekEnd", LocalDateTime.now());
                // Add digest data here

                queueEmail(pref.getUser(), "email/weekly-digest", variables, 3);
            } catch (Exception e) {
                log.error("Failed to queue weekly digest for user: {}", pref.getUser().getUsername(), e);
            }
        }
    }

    // Private helper methods

    private void sendEmail(EmailNotification notification) throws Exception {
        Context context = new Context();
        context.setVariables(notification.getTemplateVariables());
        context.setVariable("user", notification.getUser());

        String htmlContent = templateEngine.process(notification.getTemplateName(), context);

        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setFrom(fromAddress, fromName);
        helper.setTo(notification.getRecipientEmail());
        helper.setSubject(notification.getSubject());
        helper.setText(htmlContent, true);

        mailSender.send(message);
    }

    private boolean shouldSendEmail(String templateName, NotificationPreference preference) {
        return switch (templateName) {
            case "email/security-alert", "email/account-locked" -> preference.getSecurityAlertsEnabled();
            case "email/login-alert", "email/new-device-login" -> preference.getLoginAlertsEnabled();
            case "email/mfa-enabled", "email/mfa-disabled" -> preference.getMfaAlertsEnabled();
            case "email/account-change", "email/password-changed" -> preference.getAccountChangesEnabled();
            case "email/suspicious-activity" -> preference.getSuspiciousActivityEnabled();
            default -> true;
        };
    }

    private String generateSubject(String templateName) {
        return switch (templateName) {
            case "email/security-alert" -> "Security Alert - Unusual Activity Detected";
            case "email/account-locked" -> "Account Locked - Security Protection";
            case "email/login-alert" -> "New Login to Your Account";
            case "email/new-device-login" -> "New Device Login Detected";
            case "email/mfa-enabled" -> "Two-Factor Authentication Enabled";
            case "email/mfa-disabled" -> "Two-Factor Authentication Disabled";
            case "email/account-change" -> "Account Information Changed";
            case "email/password-changed" -> "Password Successfully Changed";
            case "email/suspicious-activity" -> "Suspicious Activity Detected";
            case "email/daily-digest" -> "Daily Security Digest";
            case "email/weekly-digest" -> "Weekly Security Report";
            default -> "Notification from CRUD Test";
        };
    }

    private NotificationPreference createDefaultPreference(User user) {
        NotificationPreference preference = new NotificationPreference();
        preference.setUser(user);
        preference.setEmailEnabled(true);
        preference.setSecurityAlertsEnabled(true);
        preference.setLoginAlertsEnabled(true);
        preference.setMfaAlertsEnabled(true);
        preference.setAccountChangesEnabled(true);
        preference.setSuspiciousActivityEnabled(true);
        preference.setDailyDigestEnabled(false);
        preference.setWeeklyDigestEnabled(false);
        return notificationPreferenceRepository.save(preference);
    }
}
