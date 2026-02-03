package org.example.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a JWT refresh token for token rotation and renewal.
 * Maps to the refresh_tokens table created in V6__add_refresh_tokens.sql migration.
 */
@Entity
@Table(name = "refresh_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * User who owns this token
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Unique refresh token (UUID or secure random string)
     */
    @Column(nullable = false, unique = true)
    private String token;

    /**
     * Token expiration timestamp (typically 7 days)
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * When token was revoked (NULL if still valid)
     */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    /**
     * Token that replaced this one (for token rotation)
     */
    @Column(name = "replaced_by")
    private String replacedBy;

    /**
     * IP address when token was issued
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User agent when token was issued
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * Check if token is expired.
     */
    @Transient
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Check if token is revoked.
     */
    @Transient
    public boolean isRevoked() {
        return revokedAt != null;
    }

    /**
     * Check if token is valid (not expired and not revoked).
     */
    @Transient
    public boolean isValid() {
        return !isExpired() && !isRevoked();
    }
}
