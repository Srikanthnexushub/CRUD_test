package org.example.service;

import jakarta.servlet.http.HttpServletRequest;
import org.example.entity.AuditLog;
import org.example.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

/**
 * Service interface for audit logging operations.
 */
public interface AuditLogService {

    /**
     * Log an audit event.
     *
     * @param action the action performed
     * @param userId the user ID (null for anonymous)
     * @param entityType the entity type affected
     * @param entityId the entity ID affected
     * @param oldValue previous value (JSON)
     * @param newValue new value (JSON)
     * @param request HTTP request for IP, user agent, URL
     * @param statusCode HTTP status code
     * @param errorMessage error message if failed
     * @return created audit log
     */
    AuditLog log(
            AuditAction action,
            Long userId,
            String entityType,
            Long entityId,
            String oldValue,
            String newValue,
            HttpServletRequest request,
            Integer statusCode,
            String errorMessage
    );

    /**
     * Log a successful action.
     *
     * @param action the action performed
     * @param userId the user ID
     * @param entityType the entity type
     * @param entityId the entity ID
     * @param request HTTP request
     * @return created audit log
     */
    AuditLog logSuccess(
            AuditAction action,
            Long userId,
            String entityType,
            Long entityId,
            HttpServletRequest request
    );

    /**
     * Log a failed action.
     *
     * @param action the action attempted
     * @param userId the user ID (null if unknown)
     * @param errorMessage the error message
     * @param request HTTP request
     * @return created audit log
     */
    AuditLog logFailure(
            AuditAction action,
            Long userId,
            String errorMessage,
            HttpServletRequest request
    );

    /**
     * Log authentication event.
     *
     * @param action LOGIN or LOGIN_FAILED
     * @param username the username
     * @param userId the user ID (null if login failed)
     * @param ipAddress IP address
     * @param userAgent user agent
     * @param success whether login succeeded
     * @param errorMessage error message if failed
     */
    void logAuthentication(
            AuditAction action,
            String username,
            Long userId,
            String ipAddress,
            String userAgent,
            boolean success,
            String errorMessage
    );

    /**
     * Log data change (create, update, delete).
     *
     * @param action the action (CREATE_USER, UPDATE_USER, DELETE_USER, etc.)
     * @param userId the user performing the action
     * @param entityType the entity type being changed
     * @param entityId the entity ID
     * @param oldValue old value (for updates)
     * @param newValue new value (for creates/updates)
     * @param request HTTP request
     */
    void logDataChange(
            AuditAction action,
            Long userId,
            String entityType,
            Long entityId,
            String oldValue,
            String newValue,
            HttpServletRequest request
    );

    /**
     * Get audit logs for a user.
     *
     * @param userId the user ID
     * @param pageable pagination parameters
     * @return page of audit logs
     */
    Page<AuditLog> getUserAuditLogs(Long userId, Pageable pageable);

    /**
     * Get audit logs by action.
     *
     * @param action the action
     * @param pageable pagination parameters
     * @return page of audit logs
     */
    Page<AuditLog> getAuditLogsByAction(String action, Pageable pageable);

    /**
     * Get security events (failed logins, unauthorized access, etc.).
     *
     * @param pageable pagination parameters
     * @return page of security audit logs
     */
    Page<AuditLog> getSecurityEvents(Pageable pageable);

    /**
     * Get failed actions.
     *
     * @param pageable pagination parameters
     * @return page of failed audit logs
     */
    Page<AuditLog> getFailedActions(Pageable pageable);

    /**
     * Search audit logs with filters.
     *
     * @param userId filter by user ID
     * @param action filter by action
     * @param entityType filter by entity type
     * @param startDate start date
     * @param endDate end date
     * @param pageable pagination parameters
     * @return page of filtered audit logs
     */
    Page<AuditLog> searchAuditLogs(
            Long userId,
            String action,
            String entityType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * Clean up old audit logs (scheduled task).
     *
     * @param retentionDays number of days to retain
     * @return number of logs deleted
     */
    int cleanupOldLogs(int retentionDays);

    /**
     * Extract client IP from request.
     *
     * @param request HTTP request
     * @return client IP address
     */
    String getClientIP(HttpServletRequest request);
}
