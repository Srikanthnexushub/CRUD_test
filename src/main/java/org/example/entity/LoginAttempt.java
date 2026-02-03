package org.example.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entity representing a login attempt for security monitoring and brute force protection.
 * Maps to the login_attempts table created in V4__add_login_attempts.sql migration.
 */
@Entity
@Table(name = "login_attempts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Username attempted (may not exist in users table)
     */
    @Column(nullable = false, length = 50)
    private String username;

    /**
     * IP address of login attempt
     */
    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    /**
     * Whether login was successful
     */
    @Column(nullable = false)
    private Boolean success;

    /**
     * Reason for failure (e.g., INVALID_PASSWORD, USER_NOT_FOUND, ACCOUNT_LOCKED, MFA_REQUIRED)
     */
    @Column(name = "failure_reason", length = 100)
    private String failureReason;

    /**
     * Browser user agent string
     */
    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
