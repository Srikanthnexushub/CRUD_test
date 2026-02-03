package org.example.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Custom application metrics for Prometheus monitoring.
 */
@Component
@RequiredArgsConstructor
public class CustomMetrics {

    private final MeterRegistry meterRegistry;

    // Authentication metrics
    public void recordSuccessfulLogin(String username) {
        Counter.builder("auth.login.success")
                .description("Number of successful logins")
                .tag("username", username)
                .register(meterRegistry)
                .increment();
    }

    public void recordFailedLogin(String username, String reason) {
        Counter.builder("auth.login.failure")
                .description("Number of failed login attempts")
                .tag("username", username)
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    public void recordLoginDuration(long durationMs) {
        Timer.builder("auth.login.duration")
                .description("Login processing time")
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    // MFA metrics
    public void recordMFAEnabled(String username) {
        Counter.builder("mfa.enabled")
                .description("Number of MFA enabled events")
                .tag("username", username)
                .register(meterRegistry)
                .increment();
    }

    public void recordMFAVerification(String username, boolean success) {
        String result = success ? "success" : "failure";
        Counter.builder("mfa.verification")
                .description("MFA verification attempts")
                .tag("username", username)
                .tag("result", result)
                .register(meterRegistry)
                .increment();
    }

    public void recordBackupCodeUsed(String username) {
        Counter.builder("mfa.backup_code.used")
                .description("Backup codes used")
                .tag("username", username)
                .register(meterRegistry)
                .increment();
    }

    // Account locking metrics
    public void recordAccountLocked(String username, String reason) {
        Counter.builder("security.account.locked")
                .description("Account lockout events")
                .tag("username", username)
                .tag("reason", reason)
                .register(meterRegistry)
                .increment();
    }

    public void recordAccountUnlocked(String username) {
        Counter.builder("security.account.unlocked")
                .description("Account unlock events")
                .tag("username", username)
                .register(meterRegistry)
                .increment();
    }

    // Rate limiting metrics
    public void recordRateLimitExceeded(String endpoint, String identifier) {
        Counter.builder("ratelimit.exceeded")
                .description("Rate limit exceeded events")
                .tag("endpoint", endpoint)
                .tag("identifier", identifier)
                .register(meterRegistry)
                .increment();
    }

    // API metrics
    public void recordApiCall(String endpoint, String method, int statusCode, long durationMs) {
        Timer.builder("api.request.duration")
                .description("API request processing time")
                .tag("endpoint", endpoint)
                .tag("method", method)
                .tag("status", String.valueOf(statusCode))
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);

        Counter.builder("api.request.total")
                .description("Total API requests")
                .tag("endpoint", endpoint)
                .tag("method", method)
                .tag("status", String.valueOf(statusCode))
                .register(meterRegistry)
                .increment();
    }

    // User metrics
    public void recordUserCreated(String role) {
        Counter.builder("user.created")
                .description("User creation events")
                .tag("role", role)
                .register(meterRegistry)
                .increment();
    }

    public void recordUserDeleted(String role) {
        Counter.builder("user.deleted")
                .description("User deletion events")
                .tag("role", role)
                .register(meterRegistry)
                .increment();
    }

    // Audit log metrics
    public void recordAuditEvent(String action, String entityType) {
        Counter.builder("audit.event")
                .description("Audit events logged")
                .tag("action", action)
                .tag("entity_type", entityType)
                .register(meterRegistry)
                .increment();
    }

    // Database metrics
    public void recordDatabaseQuery(String queryType, long durationMs) {
        Timer.builder("database.query.duration")
                .description("Database query execution time")
                .tag("query_type", queryType)
                .register(meterRegistry)
                .record(durationMs, TimeUnit.MILLISECONDS);
    }

    // Cache metrics
    public void recordCacheHit(String cacheName) {
        Counter.builder("cache.hit")
                .description("Cache hit events")
                .tag("cache_name", cacheName)
                .register(meterRegistry)
                .increment();
    }

    public void recordCacheMiss(String cacheName) {
        Counter.builder("cache.miss")
                .description("Cache miss events")
                .tag("cache_name", cacheName)
                .register(meterRegistry)
                .increment();
    }

    // Security metrics
    public void recordUnauthorizedAccess(String username, String resource) {
        Counter.builder("security.unauthorized_access")
                .description("Unauthorized access attempts")
                .tag("username", username)
                .tag("resource", resource)
                .register(meterRegistry)
                .increment();
    }

    public void recordSuspiciousActivity(String username, String activityType) {
        Counter.builder("security.suspicious_activity")
                .description("Suspicious activity detected")
                .tag("username", username)
                .tag("activity_type", activityType)
                .register(meterRegistry)
                .increment();
    }

    // Business metrics
    public void recordBusinessEvent(String eventType, String details) {
        Counter.builder("business.event")
                .description("Business events")
                .tag("event_type", eventType)
                .tag("details", details)
                .register(meterRegistry)
                .increment();
    }
}
