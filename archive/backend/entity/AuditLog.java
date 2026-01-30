package org.example.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_user_id", columnList = "user_id"),
    @Index(name = "idx_audit_event_type", columnList = "event_type"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp"),
    @Index(name = "idx_audit_resource", columnList = "resource_type, resource_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Who performed the action
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "username", length = 50)
    private String username;

    @Column(name = "user_role", length = 20)
    private String userRole;

    // What action was performed
    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private AuditEventType eventType;

    @Column(name = "action", nullable = false, length = 100)
    private String action;

    // When it happened
    @CreationTimestamp
    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    // Where it happened
    @Column(name = "ip_address", length = 45) // IPv6 compatible
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    // What resource was affected
    @Column(name = "resource_type", length = 50)
    private String resourceType; // e.g., "USER", "SYSTEM"

    @Column(name = "resource_id")
    private Long resourceId;

    // Details of the action
    @Column(name = "details", columnDefinition = "TEXT")
    private String details; // JSON or text description

    @Column(name = "before_state", columnDefinition = "TEXT")
    private String beforeState; // State before change (for updates)

    @Column(name = "after_state", columnDefinition = "TEXT")
    private String afterState; // State after change (for updates)

    // Result
    @Column(name = "status", nullable = false, length = 20)
    private String status; // SUCCESS, FAILURE, ERROR

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // Additional metadata
    @Column(name = "session_id", length = 100)
    private String sessionId;

    @Column(name = "request_id", length = 100)
    private String requestId;
}
