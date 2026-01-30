package org.example.repository;

import org.example.entity.RateLimitWhitelist;
import org.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for RateLimitWhitelist entity operations.
 *
 * DEBUGGING GUIDE:
 * ----------------
 * To debug rate limit whitelist queries:
 * 1. Set breakpoints in RepositoryLoggingAspect.java
 * 2. Monitor queries from RateLimitFilter (called on every request)
 * 3. Watch for whitelist bypass logic
 *
 * BREAKPOINT LOCATIONS:
 * - Line 30: findAllActive() - Debug active whitelist entries retrieval
 * - Line 33: findByIpAddressAndIsActiveTrue() - Debug IP whitelist check
 * - Line 36: findByUserAndIsActiveTrue() - Debug user whitelist check
 * - Line 39: existsByIpAddressAndIsActiveTrue() - **CRITICAL** Debug whitelist existence check (called on every request)
 * - Line 42: existsByUserAndIsActiveTrue() - Debug user whitelist existence check
 *
 * PERFORMANCE NOTES:
 * - existsByIpAddressAndIsActiveTrue() called on EVERY request - ensure it's FAST (< 10ms)
 * - Consider caching whitelist entries in memory
 */
@Repository
public interface RateLimitWhitelistRepository extends JpaRepository<RateLimitWhitelist, Long> {

    // BREAKPOINT: Debug active whitelist entries retrieval (with TTL check)
    // Watch SQL: WHERE is_active = true AND (expires_at IS NULL OR expires_at > ?)
    @Query("SELECT w FROM RateLimitWhitelist w WHERE w.isActive = true AND (w.expiresAt IS NULL OR w.expiresAt > :now)")
    List<RateLimitWhitelist> findAllActive(LocalDateTime now);

    // BREAKPOINT: Debug IP whitelist lookup
    Optional<RateLimitWhitelist> findByIpAddressAndIsActiveTrue(String ipAddress);

    // BREAKPOINT: Debug user whitelist lookup
    Optional<RateLimitWhitelist> findByUserAndIsActiveTrue(User user);

    // BREAKPOINT: **CRITICAL** Debug IP whitelist check (called on every request)
    // Watch SQL: SELECT EXISTS(SELECT 1 FROM rate_limit_whitelist WHERE ip_address = ? AND is_active = true)
    boolean existsByIpAddressAndIsActiveTrue(String ipAddress);

    // BREAKPOINT: Debug user whitelist check
    boolean existsByUserAndIsActiveTrue(User user);
}
