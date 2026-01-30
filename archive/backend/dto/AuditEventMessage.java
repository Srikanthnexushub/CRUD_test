package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.entity.AuditEventType;

import java.time.LocalDateTime;

/**
 * Real-time audit event message for WebSocket broadcasting
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEventMessage {
    private Long id;
    private Long userId;
    private String username;
    private String userRole;
    private AuditEventType eventType;
    private String action;
    private LocalDateTime timestamp;
    private String ipAddress;
    private String status;
    private String resourceType;
    private Long resourceId;
    private String severity; // HIGH, MEDIUM, LOW for frontend alerts
}
