package org.example.repository;

import org.example.entity.IPReputationCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for IPReputationCache entity operations.
 *
 * DEBUGGING GUIDE:
 * ----------------
 * To debug IP reputation cache queries:
 * 1. Set breakpoints in RepositoryLoggingAspect.java
 * 2. Monitor queries during threat intelligence checks (every login)
 * 3. Watch for cache hits vs. API calls
 *
 * BREAKPOINT LOCATIONS:
 * - Line 30: findByIpAddressAndExpiresAtAfter() - Debug cache lookup (with TTL check)
 * - Line 33: findByIpAddress() - Debug cache lookup (any entry)
 * - Line 37: deleteExpiredCache() - **CRITICAL** Debug cache cleanup (DELETE query, runs hourly)
 *
 * PERFORMANCE NOTES:
 * - findByIpAddressAndExpiresAtAfter() called on EVERY login - ensure it's FAST (< 10ms)
 * - TTL is 1 hour - reduces external API calls to AbuseIPDB
 */
@Repository
public interface IPReputationCacheRepository extends JpaRepository<IPReputationCache, Long> {

    // BREAKPOINT: Debug cache lookup with TTL validation (called on every login)
    // Watch SQL: WHERE ip_address = ? AND expires_at > ?
    Optional<IPReputationCache> findByIpAddressAndExpiresAtAfter(String ipAddress, LocalDateTime now);

    // BREAKPOINT: Debug cache lookup (any entry, regardless of expiration)
    Optional<IPReputationCache> findByIpAddress(String ipAddress);

    // BREAKPOINT: **CRITICAL** Debug cache cleanup (DELETE query, scheduled hourly)
    // Watch SQL: DELETE FROM ip_reputation_cache WHERE expires_at < ?
    @Modifying
    @Query("DELETE FROM IPReputationCache irc WHERE irc.expiresAt < :now")
    int deleteExpiredCache(LocalDateTime now);
}
