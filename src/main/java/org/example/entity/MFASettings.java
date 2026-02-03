package org.example.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing Multi-Factor Authentication settings for a user.
 * Maps to the mfa_settings table created in V3__add_mfa_settings.sql migration.
 */
@Entity
@Table(name = "mfa_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MFASettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * Base32-encoded TOTP secret key (32 characters)
     */
    @Column(nullable = false, length = 32)
    private String secret;

    /**
     * Array of one-time backup codes (encrypted)
     * Stored as PostgreSQL text array
     */
    @Column(name = "backup_codes", columnDefinition = "text[]")
    private String[] backupCodes;

    /**
     * Whether MFA has been verified with a successful TOTP code
     */
    @Column(name = "is_verified", nullable = false)
    private Boolean isVerified = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
