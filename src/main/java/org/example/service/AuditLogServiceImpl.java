package org.example.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.AuditLog;
import org.example.enums.AuditAction;
import org.example.repository.AuditLogRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementation of audit logging service for compliance and security monitoring.
 * All audit operations are async to avoid impacting application performance.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AuditLog log(
            AuditAction action,
            Long userId,
            String entityType,
            Long entityId,
            String oldValue,
            String newValue,
            HttpServletRequest request,
            Integer statusCode,
            String errorMessage
    ) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(action.name())
                    .userId(userId)
                    .entityType(entityType)
                    .entityId(entityId)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .statusCode(statusCode != null ? statusCode : HttpStatus.OK.value())
                    .errorMessage(errorMessage)
                    .build();

            if (request != null) {
                auditLog.setIpAddress(getClientIP(request));
                auditLog.setUserAgent(request.getHeader("User-Agent"));
                auditLog.setRequestUrl(request.getRequestURI());
                auditLog.setHttpMethod(request.getMethod());
            }

            AuditLog saved = auditLogRepository.save(auditLog);
            log.debug("Audit log created: {} by user: {} for entity: {}/{}",
                      action, userId, entityType, entityId);

            return saved;

        } catch (Exception e) {
            // Never throw exceptions from audit logging - log and continue
            log.error("Failed to create audit log for action: {} by user: {}", action, userId, e);
            return null;
        }
    }

    @Override
    public AuditLog logSuccess(
            AuditAction action,
            Long userId,
            String entityType,
            Long entityId,
            HttpServletRequest request
    ) {
        return log(action, userId, entityType, entityId, null, null, request,
                   HttpStatus.OK.value(), null);
    }

    @Override
    public AuditLog logFailure(
            AuditAction action,
            Long userId,
            String errorMessage,
            HttpServletRequest request
    ) {
        return log(action, userId, null, null, null, null, request,
                   HttpStatus.BAD_REQUEST.value(), errorMessage);
    }

    @Override
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAuthentication(
            AuditAction action,
            String username,
            Long userId,
            String ipAddress,
            String userAgent,
            boolean success,
            String errorMessage
    ) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .action(action.name())
                    .userId(userId)
                    .entityType("USER")
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .statusCode(success ? HttpStatus.OK.value() : HttpStatus.UNAUTHORIZED.value())
                    .errorMessage(errorMessage)
                    .newValue(username) // Store username in newValue for reference
                    .build();

            auditLogRepository.save(auditLog);

            log.info("Authentication audit log: {} for user: {} from IP: {} - Success: {}",
                     action, username, ipAddress, success);

        } catch (Exception e) {
            log.error("Failed to log authentication event for user: {}", username, e);
        }
    }

    @Override
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logDataChange(
            AuditAction action,
            Long userId,
            String entityType,
            Long entityId,
            String oldValue,
            String newValue,
            HttpServletRequest request
    ) {
        try {
            log(action, userId, entityType, entityId, oldValue, newValue, request,
                HttpStatus.OK.value(), null);

            log.info("Data change audit log: {} on {}/{} by user: {}",
                     action, entityType, entityId, userId);

        } catch (Exception e) {
            log.error("Failed to log data change for {}/{} by user: {}",
                      entityType, entityId, userId, e);
        }
    }

    @Override
    public Page<AuditLog> getUserAuditLogs(Long userId, Pageable pageable) {
        return auditLogRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Override
    public Page<AuditLog> getAuditLogsByAction(String action, Pageable pageable) {
        return auditLogRepository.findByActionOrderByCreatedAtDesc(action, pageable);
    }

    @Override
    public Page<AuditLog> getSecurityEvents(Pageable pageable) {
        return auditLogRepository.findSecurityEvents(pageable);
    }

    @Override
    public Page<AuditLog> getFailedActions(Pageable pageable) {
        return auditLogRepository.findFailedActions(pageable);
    }

    @Override
    public Page<AuditLog> searchAuditLogs(
            Long userId,
            String action,
            String entityType,
            LocalDateTime startDate,
            LocalDateTime endDate,
            Pageable pageable
    ) {
        return auditLogRepository.searchWithFilters(
                userId, action, entityType, startDate, endDate, pageable
        );
    }

    @Override
    @Transactional
    public int cleanupOldLogs(int retentionDays) {
        log.info("Cleaning up audit logs older than {} days", retentionDays);

        LocalDateTime before = LocalDateTime.now().minusDays(retentionDays);
        int deleted = auditLogRepository.deleteOldLogs(before);

        log.info("Deleted {} old audit logs", deleted);
        return deleted;
    }

    @Override
    public String getClientIP(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        // Take first IP if there are multiple (proxy chain)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip != null ? ip : "unknown";
    }

    /**
     * Convert object to JSON string for audit logging.
     *
     * @param object the object to convert
     * @return JSON string
     */
    public String toJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            log.warn("Failed to convert object to JSON for audit log", e);
            return object.toString();
        }
    }
}
