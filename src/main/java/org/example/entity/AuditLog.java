package org.example.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing an audit log entry for compliance and security monitoring.
 * Maps to the audit_logs table created in V2__add_audit_logs.sql migration.
 */
@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User who performed the action (null for anonymous actions)
     */
    @Column(name = "user_id")
    private Long userId;

    /**
     * Action performed (e.g., LOGIN, CREATE_USER, UPDATE_USER, DELETE_USER)
     */
    @Column(nullable = false, length = 50)
    private String action;

    /**
     * Type of entity affected (e.g., USER, ROLE, MFA_SETTINGS)
     */
    @Column(name = "entity_type", length = 50)
    private String entityType;

    /**
     * ID of the affected entity
     */
    @Column(name = "entity_id")
    private Long entityId;

    /**
     * Previous value (for updates) in JSON format
     */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    /**
     * New value (for creates/updates) in JSON format
     */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    /**
     * Client IP address (IPv4 or IPv6)
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * Browser user agent string
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * API endpoint that was called
     */
    @Column(name = "request_url", length = 500)
    private String requestUrl;

    /**
     * HTTP method (GET, POST, PUT, DELETE)
     */
    @Column(name = "http_method", length = 10)
    private String httpMethod;

    /**
     * HTTP response status code
     */
    @Column(name = "status_code")
    private Integer statusCode;

    /**
     * Error message if action failed
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
