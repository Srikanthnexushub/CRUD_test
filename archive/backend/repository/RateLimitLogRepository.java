package org.example.repository;

import org.example.entity.RateLimitLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for RateLimitLog entity operations.
 *
 * DEBUGGING GUIDE:
 * ----------------
 * To debug rate limiting queries:
 * 1. Set breakpoints in RepositoryLoggingAspect.java
 * 2. Monitor query execution time for performance
 * 3. Watch for high frequency queries from RateLimitFilter
 *
 * BREAKPOINT LOCATIONS:
 * - Line 30: findByWasBlockedTrueOrderByCreatedAtDesc() - Debug blocked requests
 * - Line 33: findByIpAddressAndCreatedAtAfter() - Debug IP-based rate limit checks
 * - Line 37: countBlockedInDateRange() - Debug analytics/statistics queries
 * - Line 42: findTopBlockedEndpoints() - Debug endpoint analytics (GROUP BY query)
 */
@Repository
public interface RateLimitLogRepository extends JpaRepository<RateLimitLog, Long> {

    // BREAKPOINT: Debug blocked request history
    Page<RateLimitLog> findByWasBlockedTrueOrderByCreatedAtDesc(Pageable pageable);

    // BREAKPOINT: Debug IP-based rate limit checks (time window queries)
    List<RateLimitLog> findByIpAddressAndCreatedAtAfter(String ipAddress, LocalDateTime after);

    // BREAKPOINT: Debug count aggregation for statistics
    // Watch SQL: SELECT COUNT(*) WHERE was_blocked = true AND created_at BETWEEN ? AND ?
    @Query("SELECT COUNT(rl) FROM RateLimitLog rl WHERE rl.wasBlocked = true AND rl.createdAt >= :startDate AND rl.createdAt < :endDate")
    long countBlockedInDateRange(LocalDateTime startDate, LocalDateTime endDate);

    // BREAKPOINT: Debug GROUP BY aggregation queries (endpoint analytics)
    // Watch SQL: SELECT endpoint, COUNT(*) FROM rate_limit_logs WHERE was_blocked = true GROUP BY endpoint
    // Performance: May be slow on large datasets
    @Query("SELECT rl.endpoint, COUNT(rl) as count FROM RateLimitLog rl WHERE rl.wasBlocked = true GROUP BY rl.endpoint ORDER BY count DESC")
    List<Object[]> findTopBlockedEndpoints(Pageable pageable);
}
