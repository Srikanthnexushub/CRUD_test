package org.example.repository;

import org.example.entity.AuditEventType;
import org.example.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for AuditLog entity operations.
 *
 * DEBUGGING GUIDE:
 * ----------------
 * This is the MOST COMPLEX repository with native SQL queries.
 * To debug audit log queries:
 * 1. Set breakpoints in RepositoryLoggingAspect.java
 * 2. Watch for slow queries (> 500ms) - logged automatically
 * 3. Monitor native SQL queries on line 140-145 (complex search)
 * 4. Check PostgreSQL EXPLAIN ANALYZE for query optimization
 *
 * BREAKPOINT LOCATIONS:
 * - Line 35: findAllOrderByTimestampDesc() - Debug all audit logs retrieval
 * - Line 38: findByUserId() - Debug user-specific audit trail
 * - Line 56-63: searchByUsernameOrAction() - Debug text search queries
 * - Line 140-145: searchAuditLogs() - **CRITICAL** Debug complex native SQL with multiple filters
 * - Line 169: findRecentFailedLogins() - Debug security monitoring queries
 * - Line 174: countFailedLoginAttempts() - Debug brute force detection
 *
 * PERFORMANCE NOTES:
 * - searchAuditLogs() uses native SQL with CAST operations - may be slow on large datasets
 * - Consider adding database indexes on: timestamp, user_id, event_type, status
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // Find all with pagination (simple query for basic retrieval)
    @Query("SELECT a FROM AuditLog a ORDER BY a.timestamp DESC")
    Page<AuditLog> findAllOrderByTimestampDesc(Pageable pageable);

    // Find by user
    Page<AuditLog> findByUserId(Long userId, Pageable pageable);

    Page<AuditLog> findByUsername(String username, Pageable pageable);

    // Find by event type
    Page<AuditLog> findByEventType(AuditEventType eventType, Pageable pageable);

    // Find by event type string
    @Query("SELECT a FROM AuditLog a WHERE a.eventType = :eventType ORDER BY a.timestamp DESC")
    Page<AuditLog> findByEventTypeString(@Param("eventType") AuditEventType eventType, Pageable pageable);

    // Find by resource
    Page<AuditLog> findByResourceTypeAndResourceId(String resourceType, Long resourceId, Pageable pageable);

    // Find by time range
    Page<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    // Find by status
    Page<AuditLog> findByStatus(String status, Pageable pageable);

    // Find by username containing
    @Query("SELECT a FROM AuditLog a WHERE LOWER(a.username) LIKE LOWER(CONCAT('%', :search, '%')) ORDER BY a.timestamp DESC")
    Page<AuditLog> findByUsernameContaining(@Param("search") String search, Pageable pageable);

    // Find by action containing
    @Query("SELECT a FROM AuditLog a WHERE LOWER(a.action) LIKE LOWER(CONCAT('%', :search, '%')) ORDER BY a.timestamp DESC")
    Page<AuditLog> findByActionContaining(@Param("search") String search, Pageable pageable);

    // BREAKPOINT: Debug text search queries (LIKE with wildcards)
    // Combined search (username OR action)
    // Watch SQL: WHERE LOWER(username) LIKE ? OR LOWER(action) LIKE ?
    @Query("SELECT a FROM AuditLog a WHERE " +
           "LOWER(a.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.action) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "ORDER BY a.timestamp DESC")
    Page<AuditLog> searchByUsernameOrAction(@Param("search") String search, Pageable pageable);

    // Search with event type filter
    @Query("SELECT a FROM AuditLog a WHERE " +
           "a.eventType = :eventType AND " +
           "(LOWER(a.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.action) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY a.timestamp DESC")
    Page<AuditLog> searchByEventTypeAndSearch(
        @Param("eventType") AuditEventType eventType,
        @Param("search") String search,
        Pageable pageable
    );

    // Search with status filter
    @Query("SELECT a FROM AuditLog a WHERE " +
           "a.status = :status AND " +
           "(LOWER(a.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(a.action) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY a.timestamp DESC")
    Page<AuditLog> searchByStatusAndSearch(
        @Param("status") String status,
        @Param("search") String search,
        Pageable pageable
    );

    // Date range filter
    @Query("SELECT a FROM AuditLog a WHERE " +
           "a.timestamp >= :startDate AND a.timestamp <= :endDate " +
           "ORDER BY a.timestamp DESC")
    Page<AuditLog> findByDateRange(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    // Date range with event type
    @Query("SELECT a FROM AuditLog a WHERE " +
           "a.eventType = :eventType AND " +
           "a.timestamp >= :startDate AND a.timestamp <= :endDate " +
           "ORDER BY a.timestamp DESC")
    Page<AuditLog> findByEventTypeAndDateRange(
        @Param("eventType") AuditEventType eventType,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    // Date range with status
    @Query("SELECT a FROM AuditLog a WHERE " +
           "a.status = :status AND " +
           "a.timestamp >= :startDate AND a.timestamp <= :endDate " +
           "ORDER BY a.timestamp DESC")
    Page<AuditLog> findByStatusAndDateRange(
        @Param("status") String status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );

    // BREAKPOINT: **CRITICAL** Debug complex native SQL query with multiple optional filters
    // Complex search query using native SQL for PostgreSQL compatibility
    // Watch for: CAST operations, NULL checks, dynamic filtering
    // Performance: May be slow on large datasets - check execution time in logs
    @Query(value = "SELECT * FROM audit_logs a WHERE " +
           "(:userId IS NULL OR a.user_id = CAST(:userId AS BIGINT)) AND " +
           "(:eventType IS NULL OR a.event_type = CAST(:eventType AS VARCHAR)) AND " +
           "(:status IS NULL OR a.status = CAST(:status AS VARCHAR)) AND " +
           "(:startDate IS NULL OR a.timestamp >= CAST(:startDate AS TIMESTAMP)) AND " +
           "(:endDate IS NULL OR a.timestamp <= CAST(:endDate AS TIMESTAMP)) AND " +
           "(:searchTerm IS NULL OR " +
           "LOWER(CAST(a.action AS VARCHAR)) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS VARCHAR), '%')) OR " +
           "LOWER(CAST(a.username AS VARCHAR)) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS VARCHAR), '%'))) " +
           "ORDER BY a.timestamp DESC",
           countQuery = "SELECT COUNT(*) FROM audit_logs a WHERE " +
           "(:userId IS NULL OR a.user_id = CAST(:userId AS BIGINT)) AND " +
           "(:eventType IS NULL OR a.event_type = CAST(:eventType AS VARCHAR)) AND " +
           "(:status IS NULL OR a.status = CAST(:status AS VARCHAR)) AND " +
           "(:startDate IS NULL OR a.timestamp >= CAST(:startDate AS TIMESTAMP)) AND " +
           "(:endDate IS NULL OR a.timestamp <= CAST(:endDate AS TIMESTAMP)) AND " +
           "(:searchTerm IS NULL OR " +
           "LOWER(CAST(a.action AS VARCHAR)) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS VARCHAR), '%')) OR " +
           "LOWER(CAST(a.username AS VARCHAR)) LIKE LOWER(CONCAT('%', CAST(:searchTerm AS VARCHAR), '%')))",
           nativeQuery = true)
    Page<AuditLog> searchAuditLogs(
        @Param("userId") Long userId,
        @Param("eventType") String eventType,
        @Param("status") String status,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("searchTerm") String searchTerm,
        Pageable pageable
    );

    // Count by event type for analytics
    @Query("SELECT a.eventType, COUNT(a) FROM AuditLog a GROUP BY a.eventType")
    List<Object[]> countByEventType();

    // BREAKPOINT: Debug security monitoring - failed login detection
    // Recent failed login attempts for security monitoring
    // Watch SQL: WHERE event_type = 'LOGIN_FAILURE' AND timestamp >= ?
    @Query("SELECT a FROM AuditLog a WHERE a.eventType = 'LOGIN_FAILURE' " +
           "AND a.timestamp >= :since ORDER BY a.timestamp DESC")
    List<AuditLog> findRecentFailedLogins(@Param("since") LocalDateTime since);

    // BREAKPOINT: Debug brute force attack detection (count queries)
    // Count failed login attempts by username
    // Watch SQL: SELECT COUNT(*) WHERE username = ? AND event_type = 'LOGIN_FAILURE'
    // Used by: Account lockout logic, threat intelligence
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.username = :username " +
           "AND a.eventType = 'LOGIN_FAILURE' AND a.timestamp >= :since")
    long countFailedLoginAttempts(@Param("username") String username, @Param("since") LocalDateTime since);
}
