package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.EmailNotification;
import org.example.entity.NotificationPreference;
import org.example.entity.User;
import org.example.repository.EmailNotificationRepository;
import org.example.repository.NotificationPreferenceRepository;
import org.example.repository.UserRepository;
import org.example.service.EmailService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Slf4j
public class NotificationController {

    private final NotificationPreferenceRepository preferenceRepository;
    private final EmailNotificationRepository emailRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @GetMapping("/preferences")
    public ResponseEntity<?> getPreferences(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        NotificationPreference preference = preferenceRepository.findByUser(user)
                .orElseGet(() -> createDefaultPreference(user));

        return ResponseEntity.ok(preference);
    }

    @PutMapping("/preferences")
    public ResponseEntity<?> updatePreferences(@RequestBody NotificationPreference newPreference, Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        NotificationPreference preference = preferenceRepository.findByUser(user)
                .orElseGet(() -> createDefaultPreference(user));

        // Update fields
        preference.setEmailEnabled(newPreference.getEmailEnabled());
        preference.setSecurityAlertsEnabled(newPreference.getSecurityAlertsEnabled());
        preference.setLoginAlertsEnabled(newPreference.getLoginAlertsEnabled());
        preference.setMfaAlertsEnabled(newPreference.getMfaAlertsEnabled());
        preference.setAccountChangesEnabled(newPreference.getAccountChangesEnabled());
        preference.setSuspiciousActivityEnabled(newPreference.getSuspiciousActivityEnabled());
        preference.setDailyDigestEnabled(newPreference.getDailyDigestEnabled());
        preference.setWeeklyDigestEnabled(newPreference.getWeeklyDigestEnabled());
        preference.setDigestTime(newPreference.getDigestTime());

        preferenceRepository.save(preference);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Notification preferences updated successfully");
        response.put("preferences", preference);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<?> getEmailHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Principal principal) {

        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Page<EmailNotification> emails = emailRepository.findByUserOrderByCreatedAtDesc(
                user, PageRequest.of(page, size));

        return ResponseEntity.ok(emails);
    }

    @PostMapping("/test-email")
    public ResponseEntity<?> sendTestEmail(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            emailService.sendTestEmail(user.getEmail());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Test email sent to " + user.getEmail());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to send test email", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to send test email: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    // Admin endpoints

    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> getEmailStats() {
        List<Object[]> statusCounts = emailRepository.getStatusCounts();

        Map<String, Long> stats = new HashMap<>();
        for (Object[] row : statusCounts) {
            stats.put((String) row[0], (Long) row[1]);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("stats", stats);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/admin/queue")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> getEmailQueue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        Page<EmailNotification> queue = emailRepository.findAll(PageRequest.of(page, size));

        return ResponseEntity.ok(queue);
    }

    @PostMapping("/admin/retry/{emailId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> retryFailedEmail(@PathVariable Long emailId) {
        EmailNotification email = emailRepository.findById(emailId)
                .orElseThrow(() -> new RuntimeException("Email not found"));

        if (!"FAILED".equals(email.getStatus())) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Email is not in FAILED status");
            return ResponseEntity.badRequest().body(response);
        }

        email.setStatus("PENDING");
        email.setRetryCount(0);
        email.setErrorMessage(null);
        emailRepository.save(email);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Email queued for retry");

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/admin/cancel/{emailId}")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public ResponseEntity<?> cancelEmail(@PathVariable Long emailId) {
        EmailNotification email = emailRepository.findById(emailId)
                .orElseThrow(() -> new RuntimeException("Email not found"));

        if ("SENT".equals(email.getStatus())) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Cannot cancel already sent email");
            return ResponseEntity.badRequest().body(response);
        }

        email.setStatus("CANCELLED");
        emailRepository.save(email);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Email cancelled");

        return ResponseEntity.ok(response);
    }

    // Helper method
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
        preference.setDigestTime("08:00");
        return preferenceRepository.save(preference);
    }
}
