package org.example.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.service.AccountLockService;
import org.example.service.MFAService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled tasks for maintenance and cleanup operations.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ScheduledTasks {

    private final AccountLockService accountLockService;
    private final MFAService mfaService;
    private final AuditLogService auditLogService;
    private final RefreshTokenService refreshTokenService;

    /**
     * Clean up old login attempts.
     * Runs daily at 2:00 AM.
     */
    @Scheduled(cron = "${app.scheduled.cleanup-login-attempts:0 0 2 * * *}")
    public void cleanupOldLoginAttempts() {
        log.info("Starting scheduled cleanup of old login attempts");
        try {
            int deleted = accountLockService.cleanupOldAttempts();
            log.info("Completed cleanup of old login attempts. Deleted {} records", deleted);
        } catch (Exception e) {
            log.error("Error during login attempts cleanup", e);
        }
    }

    /**
     * Clean up expired trusted devices.
     * Runs daily at 2:30 AM.
     */
    @Scheduled(cron = "${app.scheduled.cleanup-trusted-devices:0 30 2 * * *}")
    public void cleanupExpiredTrustedDevices() {
        log.info("Starting scheduled cleanup of expired trusted devices");
        try {
            int deleted = mfaService.cleanupExpiredDevices();
            log.info("Completed cleanup of expired trusted devices. Deleted {} records", deleted);
        } catch (Exception e) {
            log.error("Error during trusted devices cleanup", e);
        }
    }

    /**
     * Clean up old audit logs.
     * Runs daily at 3:00 AM.
     */
    @Scheduled(cron = "${app.scheduled.cleanup-audit-logs:0 0 3 * * *}")
    public void cleanupOldAuditLogs() {
        log.info("Starting scheduled cleanup of old audit logs");
        try {
            int deleted = auditLogService.cleanupOldLogs(365); // Keep 1 year
            log.info("Completed cleanup of old audit logs. Deleted {} records", deleted);
        } catch (Exception e) {
            log.error("Error during audit logs cleanup", e);
        }
    }

    /**
     * Clean up expired refresh tokens.
     * Runs daily at 3:30 AM.
     */
    @Scheduled(cron = "${app.scheduled.cleanup-refresh-tokens:0 30 3 * * *}")
    public void cleanupExpiredRefreshTokens() {
        log.info("Starting scheduled cleanup of expired refresh tokens");
        try {
            int deleted = refreshTokenService.cleanupExpiredTokens();
            log.info("Completed cleanup of expired refresh tokens. Deleted {} records", deleted);
        } catch (Exception e) {
            log.error("Error during refresh tokens cleanup", e);
        }
    }

    /**
     * Health check and metrics reporting.
     * Runs every hour.
     */
    @Scheduled(cron = "${app.scheduled.health-check:0 0 * * * *}")
    public void healthCheck() {
        log.debug("Performing scheduled health check");
        // Future: Add health metrics reporting
    }
}
