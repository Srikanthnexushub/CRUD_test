package org.example.logging;

import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Dedicated logger for security audit events.
 * Logs to separate security-audit.log file for compliance and forensics.
 */
@Component
public class SecurityAuditLogger {

    private static final Logger securityLog = LoggerFactory.getLogger("SECURITY_AUDIT");

    /**
     * Log authentication event.
     */
    public void logAuthentication(String username, String ipAddress, boolean success, String failureReason) {
        if (success) {
            securityLog.info("Authentication successful",
                    StructuredArguments.kv("event_type", "authentication_success"),
                    StructuredArguments.kv("username", username),
                    StructuredArguments.kv("ip_address", ipAddress)
            );
        } else {
            securityLog.warn("Authentication failed",
                    StructuredArguments.kv("event_type", "authentication_failure"),
                    StructuredArguments.kv("username", username),
                    StructuredArguments.kv("ip_address", ipAddress),
                    StructuredArguments.kv("failure_reason", failureReason)
            );
        }
    }

    /**
     * Log account lockout event.
     */
    public void logAccountLockout(String username, String reason, String ipAddress) {
        securityLog.warn("Account locked",
                StructuredArguments.kv("event_type", "account_lockout"),
                StructuredArguments.kv("username", username),
                StructuredArguments.kv("reason", reason),
                StructuredArguments.kv("ip_address", ipAddress)
        );
    }

    /**
     * Log unauthorized access attempt.
     */
    public void logUnauthorizedAccess(String username, String resource, String ipAddress) {
        securityLog.warn("Unauthorized access attempt",
                StructuredArguments.kv("event_type", "unauthorized_access"),
                StructuredArguments.kv("username", username),
                StructuredArguments.kv("resource", resource),
                StructuredArguments.kv("ip_address", ipAddress)
        );
    }

    /**
     * Log MFA event.
     */
    public void logMFAEvent(String username, String action, boolean success, String ipAddress) {
        String level = success ? "info" : "warn";
        String message = success ? "MFA " + action + " successful" : "MFA " + action + " failed";

        if (success) {
            securityLog.info(message,
                    StructuredArguments.kv("event_type", "mfa_" + action.toLowerCase()),
                    StructuredArguments.kv("username", username),
                    StructuredArguments.kv("success", true),
                    StructuredArguments.kv("ip_address", ipAddress)
            );
        } else {
            securityLog.warn(message,
                    StructuredArguments.kv("event_type", "mfa_" + action.toLowerCase()),
                    StructuredArguments.kv("username", username),
                    StructuredArguments.kv("success", false),
                    StructuredArguments.kv("ip_address", ipAddress)
            );
        }
    }

    /**
     * Log privilege escalation.
     */
    public void logPrivilegeEscalation(String username, String from, String to, String ipAddress) {
        securityLog.info("Privilege escalation",
                StructuredArguments.kv("event_type", "privilege_escalation"),
                StructuredArguments.kv("username", username),
                StructuredArguments.kv("from_role", from),
                StructuredArguments.kv("to_role", to),
                StructuredArguments.kv("ip_address", ipAddress)
        );
    }

    /**
     * Log data access event.
     */
    public void logDataAccess(String username, String entityType, Long entityId, String action, String ipAddress) {
        securityLog.info("Data access",
                StructuredArguments.kv("event_type", "data_access"),
                StructuredArguments.kv("username", username),
                StructuredArguments.kv("entity_type", entityType),
                StructuredArguments.kv("entity_id", entityId),
                StructuredArguments.kv("action", action),
                StructuredArguments.kv("ip_address", ipAddress)
        );
    }

    /**
     * Log security configuration change.
     */
    public void logConfigurationChange(String username, String configType, Map<String, Object> changes, String ipAddress) {
        securityLog.warn("Security configuration changed",
                StructuredArguments.kv("event_type", "configuration_change"),
                StructuredArguments.kv("username", username),
                StructuredArguments.kv("config_type", configType),
                StructuredArguments.kv("changes", changes),
                StructuredArguments.kv("ip_address", ipAddress)
        );
    }

    /**
     * Log suspicious activity.
     */
    public void logSuspiciousActivity(String username, String activityType, String description, String ipAddress) {
        securityLog.warn("Suspicious activity detected",
                StructuredArguments.kv("event_type", "suspicious_activity"),
                StructuredArguments.kv("username", username),
                StructuredArguments.kv("activity_type", activityType),
                StructuredArguments.kv("description", description),
                StructuredArguments.kv("ip_address", ipAddress)
        );
    }

    /**
     * Log rate limit exceeded.
     */
    public void logRateLimitExceeded(String identifier, String endpoint, String ipAddress) {
        securityLog.warn("Rate limit exceeded",
                StructuredArguments.kv("event_type", "rate_limit_exceeded"),
                StructuredArguments.kv("identifier", identifier),
                StructuredArguments.kv("endpoint", endpoint),
                StructuredArguments.kv("ip_address", ipAddress)
        );
    }
}
