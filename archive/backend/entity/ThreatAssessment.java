package org.example.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "threat_assessments", indexes = {
    @Index(name = "idx_threat_user_id", columnList = "user_id"),
    @Index(name = "idx_threat_risk_score", columnList = "risk_score"),
    @Index(name = "idx_threat_assessed_at", columnList = "assessed_at"),
    @Index(name = "idx_threat_ip_address", columnList = "ip_address"),
    @Index(name = "idx_threat_high_risk", columnList = "risk_score, assessed_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class ThreatAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnoreProperties({"passwordHash", "hibernateLazyInitializer", "handler"})
    private User user;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;

    @Column(name = "risk_level", nullable = false, length = 20)
    private String riskLevel; // LOW, MEDIUM, HIGH, CRITICAL

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "risk_factors", columnDefinition = "jsonb")
    private Map<String, Object> riskFactors;

    @Column(name = "device_fingerprint", length = 64)
    private String deviceFingerprint;

    @Column(name = "geolocation_country", length = 2)
    private String geolocationCountry;

    @Column(name = "geolocation_city", length = 100)
    private String geolocationCity;

    @Column(name = "geolocation_lat")
    private Double geolocationLat;

    @Column(name = "geolocation_lon")
    private Double geolocationLon;

    @Column(name = "is_vpn", nullable = false)
    private Boolean isVpn = false;

    @Column(name = "is_proxy", nullable = false)
    private Boolean isProxy = false;

    @Column(name = "is_tor", nullable = false)
    private Boolean isTor = false;

    @Column(name = "ip_reputation_score")
    private Integer ipReputationScore;

    @Column(name = "action_taken", length = 50)
    private String actionTaken; // ALLOWED, FLAGGED, BLOCKED, ACCOUNT_LOCKED

    @CreationTimestamp
    @Column(name = "assessed_at", nullable = false, updatable = false)
    private LocalDateTime assessedAt;
}
