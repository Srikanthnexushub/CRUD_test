package org.example.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "mfa_settings", indexes = {
    @Index(name = "idx_mfa_user_id", columnList = "user_id"),
    @Index(name = "idx_mfa_enabled", columnList = "is_enabled")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MFASettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "totp_secret", nullable = false, length = 32)
    private String totpSecret;

    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "backup_codes_generated_at")
    private LocalDateTime backupCodesGeneratedAt;

    @Column(name = "backup_codes_remaining", nullable = false)
    private Integer backupCodesRemaining = 10;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
