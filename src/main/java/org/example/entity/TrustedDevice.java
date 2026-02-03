package org.example.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a trusted device that doesn't require MFA for a period of time.
 * Maps to the trusted_devices table created in V3__add_mfa_settings.sql migration.
 */
@Entity
@Table(name = "trusted_devices")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrustedDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Unique identifier for the device (hash of fingerprint components)
     */
    @Column(name = "device_identifier", nullable = false, length = 64)
    private String deviceIdentifier;

    /**
     * Device name for user identification (e.g., "Chrome on MacBook Pro")
     */
    @Column(name = "device_name", length = 200)
    private String deviceName;

    /**
     * IP address when device was trusted
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User agent string
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    /**
     * When the trust expires (typically 30 days from creation)
     */
    @Column(name = "trusted_until", nullable = false)
    private LocalDateTime trustedUntil;

    /**
     * Last time this device was used
     */
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
