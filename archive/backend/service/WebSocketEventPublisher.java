package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.AuditEventMessage;
import org.example.entity.AuditEventType;
import org.example.entity.AuditLog;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for broadcasting real-time events to WebSocket subscribers
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Broadcast audit event to all connected clients
     */
    public void publishAuditEvent(AuditLog auditLog) {
        try {
            AuditEventMessage message = convertToMessage(auditLog);

            // Broadcast to all subscribers of /topic/audit-events
            messagingTemplate.convertAndSend("/topic/audit-events", message);

            // Also send to specific channels based on severity
            if (isHighSeverity(auditLog.getEventType())) {
                messagingTemplate.convertAndSend("/topic/security-alerts", message);
            }

            log.debug("Published audit event to WebSocket: {} - {}",
                auditLog.getEventType(), auditLog.getAction());
        } catch (Exception e) {
            log.error("Failed to publish audit event to WebSocket: {}", e.getMessage(), e);
        }
    }

    /**
     * Broadcast system notification to all connected clients
     */
    public void publishSystemNotification(String message, String severity) {
        try {
            messagingTemplate.convertAndSend("/topic/notifications",
                new SystemNotification(message, severity, java.time.LocalDateTime.now()));
        } catch (Exception e) {
            log.error("Failed to publish system notification: {}", e.getMessage(), e);
        }
    }

    /**
     * Send user-specific notification
     */
    public void publishUserNotification(String username, String message, String severity) {
        try {
            messagingTemplate.convertAndSend("/topic/user/" + username,
                new SystemNotification(message, severity, java.time.LocalDateTime.now()));
        } catch (Exception e) {
            log.error("Failed to publish user notification: {}", e.getMessage(), e);
        }
    }

    private AuditEventMessage convertToMessage(AuditLog auditLog) {
        return AuditEventMessage.builder()
            .id(auditLog.getId())
            .userId(auditLog.getUserId())
            .username(auditLog.getUsername())
            .userRole(auditLog.getUserRole())
            .eventType(auditLog.getEventType())
            .action(auditLog.getAction())
            .timestamp(auditLog.getTimestamp())
            .ipAddress(auditLog.getIpAddress())
            .status(auditLog.getStatus())
            .resourceType(auditLog.getResourceType())
            .resourceId(auditLog.getResourceId())
            .severity(determineSeverity(auditLog.getEventType()))
            .build();
    }

    private String determineSeverity(AuditEventType eventType) {
        return switch (eventType) {
            case LOGIN_FAILURE, ACCESS_DENIED, SUSPICIOUS_ACTIVITY -> "HIGH";
            case USER_DELETED, USER_UPDATED -> "MEDIUM";
            default -> "LOW";
        };
    }

    private boolean isHighSeverity(AuditEventType eventType) {
        return eventType == AuditEventType.LOGIN_FAILURE ||
               eventType == AuditEventType.ACCESS_DENIED ||
               eventType == AuditEventType.SUSPICIOUS_ACTIVITY;
    }

    // Inner class for system notifications
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class SystemNotification {
        private String message;
        private String severity;
        private java.time.LocalDateTime timestamp;
    }
}
