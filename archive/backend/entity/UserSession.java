package org.example.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity for tracking user sessions with detailed device and location information
 */
@Entity
@Table(name = "user_sessions", indexes = {
    @Index(name = "idx_session_user_id", columnList = "user_id"),
    @Index(name = "idx_session_token", columnList = "session_token"),
    @Index(name = "idx_session_active", columnList = "is_active"),
    @Index(name = "idx_session_created", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "username", nullable = false)
    private String username;

    @Column(name = "session_token", unique = true, nullable = false)
    private String sessionToken;

    // Device Information
    @Column(name = "device_fingerprint")
    private String deviceFingerprint;

    @Column(name = "device_type")
    private String deviceType; // Desktop, Mobile, Tablet

    @Column(name = "browser")
    private String browser;

    @Column(name = "browser_version")
    private String browserVersion;

    @Column(name = "os")
    private String operatingSystem;

    @Column(name = "os_version")
    private String osVersion;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    // Location Information
    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "country")
    private String country;

    @Column(name = "city")
    private String city;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "timezone")
    private String timezone;

    // Session Status
    @Column(name = "is_active")
    private Boolean isActive;

    @Column(name = "last_activity")
    private LocalDateTime lastActivity;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "terminated_at")
    private LocalDateTime terminatedAt;

    @Column(name = "termination_reason")
    private String terminationReason; // LOGOUT, TIMEOUT, FORCED, EXPIRED

    // Risk Assessment
    @Column(name = "risk_score")
    private Integer riskScore; // 0-100

    @Column(name = "is_suspicious")
    private Boolean isSuspicious;

    @Column(name = "suspicious_reasons", columnDefinition = "TEXT")
    private String suspiciousReasons;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
