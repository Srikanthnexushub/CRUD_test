package org.example.repository;

import org.example.entity.UserSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for UserSession entity operations.
 *
 * DEBUGGING GUIDE:
 * ----------------
 * To debug session management queries:
 * 1. Set breakpoints in RepositoryLoggingAspect.java
 * 2. Monitor session queries during login/logout
 * 3. Watch for suspicious session detection queries
 *
 * BREAKPOINT LOCATIONS:
 * - Line 37: findBySessionTokenAndIsActiveTrue() - Debug JWT token validation
 * - Line 40: findByUserIdAndIsActiveTrue() - Debug active sessions for user
 * - Line 52: findSuspiciousActiveSessions() - Debug threat intelligence integration
 * - Line 57: findExpiredSessions() - Debug session cleanup (scheduled task)
 * - Line 62: findMultipleDeviceSessionsFromSameIp() - Debug account sharing detection
 * - Line 77: findRecentSessionsByUserId() - Debug threat analysis (device fingerprinting)
 *
 * PERFORMANCE NOTES:
 * - findExpiredSessions() runs every minute - ensure it's fast (< 100ms)
 * - Consider adding index on: session_token, user_id, is_active, expires_at
 */
@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    // BREAKPOINT: Debug JWT token validation (called on every authenticated request)
    // Find active session by token
    // Watch SQL: WHERE session_token = ? AND is_active = true
    Optional<UserSession> findBySessionTokenAndIsActiveTrue(String sessionToken);

    // BREAKPOINT: Debug active sessions retrieval (user dashboard)
    // Find all active sessions for a user
    List<UserSession> findByUserIdAndIsActiveTrue(Long userId);

    // BREAKPOINT: Debug session count (concurrent session limits)
    // Count active sessions for a user
    long countByUserIdAndIsActiveTrue(Long userId);

    // BREAKPOINT: Debug session history with pagination
    // Find all sessions for a user
    Page<UserSession> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // BREAKPOINT: Debug device-based session lookup (trusted device checks)
    // Find sessions by device fingerprint
    List<UserSession> findByDeviceFingerprintAndIsActiveTrue(String deviceFingerprint);

    // BREAKPOINT: Debug suspicious session detection (threat intelligence)
    // Find suspicious sessions
    // Watch SQL: WHERE is_suspicious = true AND is_active = true
    @Query("SELECT s FROM UserSession s WHERE s.isSuspicious = true AND s.isActive = true ORDER BY s.createdAt DESC")
    List<UserSession> findSuspiciousActiveSessions();

    // BREAKPOINT: **CRITICAL** Debug session expiration cleanup (scheduled task - runs every minute)
    // Find sessions that need to be expired
    // Watch SQL: WHERE is_active = true AND expires_at < ?
    // Performance: Ensure this is FAST (< 100ms)
    @Query("SELECT s FROM UserSession s WHERE s.isActive = true AND s.expiresAt < :now")
    List<UserSession> findExpiredSessions(@Param("now") LocalDateTime now);

    // BREAKPOINT: Debug account sharing detection (GROUP BY query)
    // Find sessions from same IP but different devices (potential account sharing)
    // Watch SQL: SELECT * FROM user_sessions WHERE ip_address = ? AND is_active = true GROUP BY device_fingerprint HAVING COUNT(*) > 1
    @Query("SELECT s FROM UserSession s WHERE s.ipAddress = :ipAddress AND s.isActive = true GROUP BY s.deviceFingerprint HAVING COUNT(s.deviceFingerprint) > 1")
    List<UserSession> findMultipleDeviceSessionsFromSameIp(@Param("ipAddress") String ipAddress);

    // Find active sessions count
    @Query("SELECT COUNT(s) FROM UserSession s WHERE s.isActive = true")
    long countActiveSessions();

    // Find sessions by country
    List<UserSession> findByCountryAndIsActiveTrue(String country);

    // Find all sessions in a date range
    @Query("SELECT s FROM UserSession s WHERE s.createdAt >= :startDate AND s.createdAt <= :endDate ORDER BY s.createdAt DESC")
    Page<UserSession> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate,
                                      Pageable pageable);

    // Find recent sessions for threat analysis
    @Query("SELECT us FROM UserSession us WHERE us.userId = :userId AND us.createdAt > :since ORDER BY us.createdAt DESC")
    List<UserSession> findRecentSessionsByUserId(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    // Find all sessions by user ID
    List<UserSession> findByUserId(Long userId);
}
