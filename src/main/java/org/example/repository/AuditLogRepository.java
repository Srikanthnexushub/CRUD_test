package org.example.repository;

import org.example.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for Audit Log entity operations.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Find audit logs by user ID.
     *
     * @param userId the user ID
     * @param pageable pagination parameters
     * @return page of audit logs
     */
    Page<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find audit logs by action.
     *
     * @param action the action
     * @param pageable pagination parameters
     * @return page of audit logs
     */
    Page<AuditLog> findByActionOrderByCreatedAtDesc(String action, Pageable pageable);

    /**
     * Find audit logs by entity type and ID.
     *
     * @param entityType the entity type
     * @param entityId the entity ID
     * @return list of audit logs
     */
    List<AuditLog> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(String entityType, Long entityId);

    /**
     * Find audit logs by IP address.
     *
     * @param ipAddress the IP address
     * @param pageable pagination parameters
     * @return page of audit logs
     */
    Page<AuditLog> findByIpAddressOrderByCreatedAtDesc(String ipAddress, Pageable pageable);

    /**
     * Find audit logs within date range.
     *
     * @param startDate start date
     * @param endDate end date
     * @param pageable pagination parameters
     * @return page of audit logs
     */
    @Query("SELECT al FROM AuditLog al WHERE al.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY al.createdAt DESC")
    Page<AuditLog> findByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );

    /**
     * Find failed actions (error_message is not null).
     *
     * @param pageable pagination parameters
     * @return page of failed audit logs
     */
    @Query("SELECT al FROM AuditLog al WHERE al.errorMessage IS NOT NULL " +
           "ORDER BY al.createdAt DESC")
    Page<AuditLog> findFailedActions(Pageable pageable);

    /**
     * Find security-related events.
     *
     * @param pageable pagination parameters
     * @return page of security audit logs
     */
    @Query("SELECT al FROM AuditLog al WHERE al.action IN " +
           "('LOGIN_FAILED', 'ACCOUNT_LOCKED', 'UNAUTHORIZED_ACCESS', 'PERMISSION_DENIED', " +
           "'SUSPICIOUS_ACTIVITY', 'BRUTE_FORCE_ATTEMPT', 'IP_BLOCKED', 'MFA_FAILED') " +
           "ORDER BY al.createdAt DESC")
    Page<AuditLog> findSecurityEvents(Pageable pageable);

    /**
     * Count audit logs by user within time range.
     *
     * @param userId the user ID
     * @param since timestamp to count from
     * @return count of audit logs
     */
    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.userId = :userId " +
           "AND al.createdAt > :since")
    long countByUserSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    /**
     * Count audit logs by action within time range.
     *
     * @param action the action
     * @param since timestamp to count from
     * @return count of audit logs
     */
    @Query("SELECT COUNT(al) FROM AuditLog al WHERE al.action = :action " +
           "AND al.createdAt > :since")
    long countByActionSince(@Param("action") String action, @Param("since") LocalDateTime since);

    /**
     * Delete old audit logs (cleanup).
     *
     * @param before delete logs older than this timestamp
     * @return number of deleted records
     */
    @Modifying
    @Query("DELETE FROM AuditLog al WHERE al.createdAt < :before")
    int deleteOldLogs(@Param("before") LocalDateTime before);

    /**
     * Search audit logs with filters.
     *
     * @param userId filter by user ID (nullable)
     * @param action filter by action (nullable)
     * @param entityType filter by entity type (nullable)
     * @param startDate start date (nullable)
     * @param endDate end date (nullable)
     * @param pageable pagination parameters
     * @return page of filtered audit logs
     */
    @Query("SELECT al FROM AuditLog al WHERE " +
           "(:userId IS NULL OR al.userId = :userId) AND " +
           "(:action IS NULL OR al.action = :action) AND " +
           "(:entityType IS NULL OR al.entityType = :entityType) AND " +
           "(:startDate IS NULL OR al.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR al.createdAt <= :endDate) " +
           "ORDER BY al.createdAt DESC")
    Page<AuditLog> searchWithFilters(
            @Param("userId") Long userId,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}
