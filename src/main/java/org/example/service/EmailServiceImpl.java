package org.example.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.EmailRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Email Service Implementation
 * Handles all email notifications with Thymeleaf templates
 * Includes circuit breaker and retry for resilience
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final SpringTemplateEngine templateEngine;

    @Value("${app.mail.from}")
    private String fromAddress;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${app.mail.enabled:true}")
    private boolean emailEnabled;

    @Value("${server.port:8080}")
    private String serverPort;

    private static final String BASE_URL = "http://localhost";

    @Override
    @Async
    @CircuitBreaker(name = "emailService", fallbackMethod = "emailFallback")
    @Retry(name = "emailService")
    public void sendSimpleEmail(String to, String subject, String text) {
        if (!emailEnabled) {
            log.info("Email disabled. Would send to {}: {}", to, subject);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);

            mailSender.send(message);
            log.info("Simple email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send simple email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    @Async
    @CircuitBreaker(name = "emailService", fallbackMethod = "emailFallback")
    @Retry(name = "emailService")
    public void sendTemplateEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        if (!emailEnabled) {
            log.info("Email disabled. Would send to {}: {}", to, subject);
            return;
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            // Set email properties
            helper.setFrom(fromAddress, fromName);
            helper.setTo(to);
            helper.setSubject(subject);

            // Process template
            Context context = new Context();
            context.setVariables(variables);
            String htmlContent = templateEngine.process(templateName, context);

            helper.setText(htmlContent, true);

            mailSender.send(mimeMessage);
            log.info("Template email '{}' sent successfully to: {}", templateName, to);
        } catch (MessagingException e) {
            log.error("Failed to send template email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        } catch (Exception e) {
            log.error("Unexpected error sending email to: {}", to, e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    @Override
    public void sendWelcomeEmail(String to, String username) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("appName", fromName);
        variables.put("loginUrl", BASE_URL + ":" + serverPort + "/login");
        variables.put("year", LocalDateTime.now().getYear());

        sendTemplateEmail(to, "Welcome to " + fromName, "welcome-email", variables);
    }

    @Override
    public void sendPasswordResetEmail(String to, String username, String resetToken) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("resetLink", BASE_URL + ":" + serverPort + "/reset-password?token=" + resetToken);
        variables.put("expirationMinutes", 30);
        variables.put("appName", fromName);
        variables.put("year", LocalDateTime.now().getYear());

        sendTemplateEmail(to, "Password Reset Request", "password-reset-email", variables);
    }

    @Override
    public void sendAccountLockedEmail(String to, String username, String reason) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("reason", reason);
        variables.put("appName", fromName);
        variables.put("supportEmail", fromAddress);
        variables.put("year", LocalDateTime.now().getYear());

        sendTemplateEmail(to, "Account Locked - Security Alert", "account-locked-email", variables);
    }

    @Override
    public void sendAccountUnlockedEmail(String to, String username) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("appName", fromName);
        variables.put("loginUrl", BASE_URL + ":" + serverPort + "/login");
        variables.put("year", LocalDateTime.now().getYear());

        sendTemplateEmail(to, "Account Unlocked", "account-unlocked-email", variables);
    }

    @Override
    public void sendMFAEnabledEmail(String to, String username) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("appName", fromName);
        variables.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        variables.put("year", LocalDateTime.now().getYear());

        sendTemplateEmail(to, "Two-Factor Authentication Enabled", "mfa-enabled-email", variables);
    }

    @Override
    public void sendMFADisabledEmail(String to, String username) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("appName", fromName);
        variables.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        variables.put("supportEmail", fromAddress);
        variables.put("year", LocalDateTime.now().getYear());

        sendTemplateEmail(to, "Two-Factor Authentication Disabled", "mfa-disabled-email", variables);
    }

    @Override
    public void sendPasswordChangedEmail(String to, String username) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("appName", fromName);
        variables.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        variables.put("supportEmail", fromAddress);
        variables.put("year", LocalDateTime.now().getYear());

        sendTemplateEmail(to, "Password Changed Successfully", "password-changed-email", variables);
    }

    @Override
    public void sendSuspiciousLoginEmail(String to, String username, String ipAddress, String location, String device) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("ipAddress", ipAddress);
        variables.put("location", location);
        variables.put("device", device);
        variables.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        variables.put("appName", fromName);
        variables.put("securityUrl", BASE_URL + ":" + serverPort + "/security");
        variables.put("year", LocalDateTime.now().getYear());

        sendTemplateEmail(to, "Suspicious Login Activity Detected", "suspicious-login-email", variables);
    }

    @Override
    public void sendNewDeviceLoginEmail(String to, String username, String device, String ipAddress, String location) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("username", username);
        variables.put("device", device);
        variables.put("ipAddress", ipAddress);
        variables.put("location", location);
        variables.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        variables.put("appName", fromName);
        variables.put("year", LocalDateTime.now().getYear());

        sendTemplateEmail(to, "New Device Login Detected", "new-device-login-email", variables);
    }

    @Override
    public void sendNotificationEmail(EmailRequest request) {
        if (request.isHtml() && request.getTemplateName() != null) {
            sendTemplateEmail(request.getTo(), request.getSubject(), request.getTemplateName(), request.getVariables());
        } else {
            sendSimpleEmail(request.getTo(), request.getSubject(), request.getText());
        }
    }

    @Override
    public boolean testEmailConfiguration(String testRecipient) {
        try {
            sendSimpleEmail(
                testRecipient,
                "Test Email - Configuration Check",
                "This is a test email to verify email configuration is working correctly."
            );
            return true;
        } catch (Exception e) {
            log.error("Email configuration test failed", e);
            return false;
        }
    }

    /**
     * Fallback method for circuit breaker
     */
    private void emailFallback(String to, String subject, String text, Exception e) {
        log.warn("Email circuit breaker activated. Email not sent to: {}. Error: {}", to, e.getMessage());
    }

    private void emailFallback(String to, String subject, String templateName, Map<String, Object> variables, Exception e) {
        log.warn("Email circuit breaker activated. Template email '{}' not sent to: {}. Error: {}",
                 templateName, to, e.getMessage());
    }
}
