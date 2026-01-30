package org.example.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.AuditEventType;
import org.example.entity.AuditLog;
import org.example.entity.User;
import org.example.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final WebSocketEventPublisher webSocketEventPublisher;

    /**
     * Create an audit log entry asynchronously
     * Uses REQUIRES_NEW to ensure audit log is saved even if parent transaction fails
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(AuditLog auditLog) {
        try {
            // Add request context if available
            enrichWithRequestContext(auditLog);
            AuditLog savedLog = auditLogRepository.save(auditLog);
            log.debug("Audit log created: {} - {} by {}",
                auditLog.getEventType(), auditLog.getAction(), auditLog.getUsername());

            // Broadcast event to WebSocket subscribers in real-time
            webSocketEventPublisher.publishAuditEvent(savedLog);
        } catch (Exception e) {
            log.error("Failed to save audit log: {}", e.getMessage(), e);
            // Don't throw exception to avoid breaking the main business logic
        }
    }

    /**
     * Log authentication success
     */
    public void logLoginSuccess(String username, String userRole, Long userId) {
        AuditLog auditLog = createBaseAuditLog(userId, username, userRole, AuditEventType.LOGIN_SUCCESS);
        auditLog.setAction("User logged in successfully");
        auditLog.setStatus("SUCCESS");
        log(auditLog);
    }

    /**
     * Log authentication failure
     */
    public void logLoginFailure(String username, String reason) {
        AuditLog auditLog = createBaseAuditLog(null, username, null, AuditEventType.LOGIN_FAILURE);
        auditLog.setAction("Failed login attempt");
        auditLog.setStatus("FAILURE");
        auditLog.setErrorMessage(reason);
        auditLog.setDetails("Username: " + username + ", Reason: " + reason);
        log(auditLog);
    }

    /**
     * Log logout
     */
    public void logLogout(String username, String userRole, Long userId) {
        AuditLog auditLog = createBaseAuditLog(userId, username, userRole, AuditEventType.LOGOUT);
        auditLog.setAction("User logged out");
        auditLog.setStatus("SUCCESS");
        log(auditLog);
    }

    /**
     * Log user creation
     */
    public void logUserCreated(User createdBy, User newUser) {
        AuditLog auditLog = createBaseAuditLog(
            createdBy != null ? createdBy.getId() : null,
            createdBy != null ? createdBy.getUsername() : "system",
            createdBy != null ? createdBy.getRole().name() : "SYSTEM",
            AuditEventType.USER_CREATED
        );
        auditLog.setAction("Created new user: " + newUser.getUsername());
        auditLog.setResourceType("USER");
        auditLog.setResourceId(newUser.getId());
        auditLog.setAfterState(formatUserState(newUser));
        auditLog.setStatus("SUCCESS");
        log(auditLog);
    }

    /**
     * Log user update
     */
    public void logUserUpdated(User updatedBy, User oldUser, User newUser) {
        AuditLog auditLog = createBaseAuditLog(
            updatedBy.getId(),
            updatedBy.getUsername(),
            updatedBy.getRole().name(),
            AuditEventType.USER_UPDATED
        );
        auditLog.setAction("Updated user: " + newUser.getUsername());
        auditLog.setResourceType("USER");
        auditLog.setResourceId(newUser.getId());
        auditLog.setBeforeState(formatUserState(oldUser));
        auditLog.setAfterState(formatUserState(newUser));
        auditLog.setDetails(buildUpdateDetails(oldUser, newUser));
        auditLog.setStatus("SUCCESS");
        log(auditLog);
    }

    /**
     * Log user deletion
     */
    public void logUserDeleted(User deletedBy, User deletedUser) {
        AuditLog auditLog = createBaseAuditLog(
            deletedBy.getId(),
            deletedBy.getUsername(),
            deletedBy.getRole().name(),
            AuditEventType.USER_DELETED
        );
        auditLog.setAction("Deleted user: " + deletedUser.getUsername());
        auditLog.setResourceType("USER");
        auditLog.setResourceId(deletedUser.getId());
        auditLog.setBeforeState(formatUserState(deletedUser));
        auditLog.setStatus("SUCCESS");
        log(auditLog);
    }

    /**
     * Log user view
     */
    public void logUserViewed(User viewer, Long viewedUserId) {
        AuditLog auditLog = createBaseAuditLog(
            viewer.getId(),
            viewer.getUsername(),
            viewer.getRole().name(),
            AuditEventType.USER_VIEWED
        );
        auditLog.setAction("Viewed user details");
        auditLog.setResourceType("USER");
        auditLog.setResourceId(viewedUserId);
        auditLog.setStatus("SUCCESS");
        log(auditLog);
    }

    /**
     * Log access denied
     */
    public void logAccessDenied(String username, String resource, String reason) {
        AuditLog auditLog = createBaseAuditLog(null, username, null, AuditEventType.ACCESS_DENIED);
        auditLog.setAction("Access denied to " + resource);
        auditLog.setStatus("FAILURE");
        auditLog.setErrorMessage(reason);
        auditLog.setDetails("Resource: " + resource + ", Reason: " + reason);
        log(auditLog);
    }

    /**
     * Log suspicious activity
     */
    public void logSuspiciousActivity(String username, String activity, String details) {
        AuditLog auditLog = createBaseAuditLog(null, username, null, AuditEventType.SUSPICIOUS_ACTIVITY);
        auditLog.setAction("Suspicious activity detected: " + activity);
        auditLog.setStatus("WARNING");
        auditLog.setDetails(details);
        log(auditLog);
    }

    /**
     * Get all audit logs with pagination (no filtering)
     */
    public Page<AuditLog> getAllAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllOrderByTimestampDesc(pageable);
    }

    /**
     * Retrieve audit logs with filtering and pagination
     * Combines multiple filters intelligently
     */
    public Page<AuditLog> searchAuditLogs(
        Long userId,
        AuditEventType eventType,
        String status,
        LocalDateTime startDate,
        LocalDateTime endDate,
        String searchTerm,
        Pageable pageable
    ) {
        // Normalize inputs
        if (searchTerm != null && searchTerm.trim().isEmpty()) {
            searchTerm = null;
        }
        if (status != null && status.trim().isEmpty()) {
            status = null;
        }

        // Handle date range filters with other filters
        boolean hasDateRange = (startDate != null && endDate != null);
        boolean hasSearch = (searchTerm != null);
        boolean hasEventType = (eventType != null);
        boolean hasStatus = (status != null);
        boolean hasUserId = (userId != null);

        // Priority combinations for date range
        if (hasDateRange) {
            if (hasEventType) {
                return auditLogRepository.findByEventTypeAndDateRange(eventType, startDate, endDate, pageable);
            }
            if (hasStatus) {
                return auditLogRepository.findByStatusAndDateRange(status, startDate, endDate, pageable);
            }
            // Date range only
            return auditLogRepository.findByDateRange(startDate, endDate, pageable);
        }

        // Handle search with other filters
        if (hasSearch) {
            if (hasEventType) {
                return auditLogRepository.searchByEventTypeAndSearch(eventType, searchTerm, pageable);
            }
            if (hasStatus) {
                return auditLogRepository.searchByStatusAndSearch(status, searchTerm, pageable);
            }
            // Search only (username OR action)
            return auditLogRepository.searchByUsernameOrAction(searchTerm, pageable);
        }

        // Single filter queries
        if (hasEventType) {
            return auditLogRepository.findByEventType(eventType, pageable);
        }

        if (hasStatus) {
            return auditLogRepository.findByStatus(status, pageable);
        }

        if (hasUserId) {
            return auditLogRepository.findByUserId(userId, pageable);
        }

        // No filters, return all
        return auditLogRepository.findAllOrderByTimestampDesc(pageable);
    }

    /**
     * Get all audit logs for a specific user
     */
    public Page<AuditLog> getAuditLogsByUserId(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserId(userId, pageable);
    }

    /**
     * Check for brute force attempts
     */
    public boolean isBruteForceAttempt(String username, int maxAttempts, int minutesWindow) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(minutesWindow);
        long failedAttempts = auditLogRepository.countFailedLoginAttempts(username, since);

        if (failedAttempts >= maxAttempts) {
            logSuspiciousActivity(username, "Brute force attempt",
                String.format("Failed login attempts: %d in %d minutes", failedAttempts, minutesWindow));
            return true;
        }
        return false;
    }

    // Helper methods

    private AuditLog createBaseAuditLog(Long userId, String username, String userRole, AuditEventType eventType) {
        return AuditLog.builder()
            .userId(userId)
            .username(username)
            .userRole(userRole)
            .eventType(eventType)
            .requestId(UUID.randomUUID().toString())
            .build();
    }

    private void enrichWithRequestContext(AuditLog auditLog) {
        try {
            ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                auditLog.setIpAddress(getClientIpAddress(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
                auditLog.setSessionId(request.getSession(false) != null ?
                    request.getSession(false).getId() : null);
            }
        } catch (Exception e) {
            log.debug("Could not enrich audit log with request context: {}", e.getMessage());
        }
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    private String formatUserState(User user) {
        return String.format("{id: %d, username: '%s', email: '%s', role: '%s'}",
            user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }

    private String buildUpdateDetails(User oldUser, User newUser) {
        StringBuilder details = new StringBuilder("Changes: ");

        if (!oldUser.getUsername().equals(newUser.getUsername())) {
            details.append(String.format("username '%s' -> '%s'; ",
                oldUser.getUsername(), newUser.getUsername()));
        }

        if (!oldUser.getEmail().equals(newUser.getEmail())) {
            details.append(String.format("email '%s' -> '%s'; ",
                oldUser.getEmail(), newUser.getEmail()));
        }

        if (!oldUser.getRole().equals(newUser.getRole())) {
            details.append(String.format("role '%s' -> '%s'; ",
                oldUser.getRole(), newUser.getRole()));
        }

        return details.toString();
    }
}
