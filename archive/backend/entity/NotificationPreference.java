package org.example.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_preferences", indexes = {
    @Index(name = "idx_notif_user_id", columnList = "user_id", unique = true)
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "email_enabled", nullable = false)
    private Boolean emailEnabled = true;

    @Column(name = "security_alerts_enabled", nullable = false)
    private Boolean securityAlertsEnabled = true;

    @Column(name = "login_alerts_enabled", nullable = false)
    private Boolean loginAlertsEnabled = true;

    @Column(name = "mfa_alerts_enabled", nullable = false)
    private Boolean mfaAlertsEnabled = true;

    @Column(name = "account_changes_enabled", nullable = false)
    private Boolean accountChangesEnabled = true;

    @Column(name = "suspicious_activity_enabled", nullable = false)
    private Boolean suspiciousActivityEnabled = true;

    @Column(name = "daily_digest_enabled", nullable = false)
    private Boolean dailyDigestEnabled = false;

    @Column(name = "weekly_digest_enabled", nullable = false)
    private Boolean weeklyDigestEnabled = false;

    @Column(name = "digest_time", length = 5)
    private String digestTime = "08:00"; // HH:MM format

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
