package org.example.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Threat Intelligence Entity
 * Stores information about potentially malicious IPs and security threats
 */
@Entity
@Table(name = "threat_intelligence", indexes = {
    @Index(name = "idx_threat_ip_address", columnList = "ip_address"),
    @Index(name = "idx_threat_risk_score", columnList = "risk_score"),
    @Index(name = "idx_threat_expires_at", columnList = "expires_at"),
    @Index(name = "idx_threat_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThreatIntelligence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "risk_score", nullable = false)
    @Builder.Default
    private Integer riskScore = 0;

    @Column(name = "threat_type", length = 50)
    private String threatType;

    @Column(name = "threat_category", length = 50)
    private String threatCategory;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "is_vpn")
    @Builder.Default
    private Boolean isVpn = false;

    @Column(name = "is_proxy")
    @Builder.Default
    private Boolean isProxy = false;

    @Column(name = "is_tor")
    @Builder.Default
    private Boolean isTor = false;

    @Column(name = "is_datacenter")
    @Builder.Default
    private Boolean isDatacenter = false;

    @Column(name = "is_blacklisted")
    @Builder.Default
    private Boolean isBlacklisted = false;

    @Column(name = "failed_login_count")
    @Builder.Default
    private Integer failedLoginCount = 0;

    @Column(name = "suspicious_activity_count")
    @Builder.Default
    private Integer suspiciousActivityCount = 0;

    @Column(name = "last_seen", nullable = false)
    private LocalDateTime lastSeen;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "source", length = 50)
    private String source;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastSeen = LocalDateTime.now();
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusDays(30);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        lastSeen = LocalDateTime.now();
    }

    /**
     * Calculate overall risk level based on risk score
     */
    public String getRiskLevel() {
        if (riskScore >= 80) return "CRITICAL";
        if (riskScore >= 60) return "HIGH";
        if (riskScore >= 40) return "MEDIUM";
        if (riskScore >= 20) return "LOW";
        return "MINIMAL";
    }

    /**
     * Check if IP should be blocked
     */
    public boolean shouldBlock() {
        return isBlacklisted || riskScore >= 80 || isTor ||
               (isProxy && riskScore >= 60) || failedLoginCount >= 10;
    }

    /**
     * Increment failed login count and update risk score
     */
    public void recordFailedLogin() {
        failedLoginCount++;
        riskScore = Math.min(100, riskScore + 5);
    }

    /**
     * Increment suspicious activity count and update risk score
     */
    public void recordSuspiciousActivity() {
        suspiciousActivityCount++;
        riskScore = Math.min(100, riskScore + 10);
    }
}
