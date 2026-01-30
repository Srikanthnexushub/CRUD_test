package org.example.repository;

import org.example.entity.EmailNotification;
import org.example.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for EmailNotification entity operations.
 *
 * DEBUGGING GUIDE:
 * ----------------
 * To debug email notification queries:
 * 1. Set breakpoints in RepositoryLoggingAspect.java
 * 2. Monitor query frequency from EmailService scheduled tasks
 * 3. Watch for pending email queue queries (every 5 minutes)
 *
 * BREAKPOINT LOCATIONS:
 * - Line 35: findByUserOrderByCreatedAtDesc() - Debug user notification history
 * - Line 39: findPendingEmails() - **CRITICAL** Debug email queue processing
 * - Line 43: findFailedEmailsForRetry() - Debug email retry logic
 * - Line 46: countByStatus() - Debug email statistics
 * - Line 50: getStatusCounts() - Debug email status distribution (GROUP BY)
 *
 * PERFORMANCE NOTES:
 * - findPendingEmails() runs every 5 minutes - ensure it's fast (< 100ms)
 * - Use ORDER BY priority ASC to process high-priority emails first
 */
@Repository
public interface EmailNotificationRepository extends JpaRepository<EmailNotification, Long> {

    // BREAKPOINT: Debug user notification history
    Page<EmailNotification> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    // BREAKPOINT: **CRITICAL** Debug email queue processing (scheduled task)
    // Watch SQL: WHERE status = 'PENDING' AND (scheduled_for IS NULL OR scheduled_for <= ?)
    // Used by: EmailService.processPendingEmails() - runs every 5 minutes
    @Query("SELECT en FROM EmailNotification en WHERE en.status = 'PENDING' AND (en.scheduledFor IS NULL OR en.scheduledFor <= :now) ORDER BY en.priority ASC, en.createdAt ASC")
    List<EmailNotification> findPendingEmails(LocalDateTime now, Pageable pageable);

    // BREAKPOINT: Debug email retry logic (exponential backoff)
    // Watch SQL: WHERE status = 'FAILED' AND retry_count < max_retries
    @Query("SELECT en FROM EmailNotification en WHERE en.status = 'FAILED' AND en.retryCount < en.maxRetries ORDER BY en.createdAt ASC")
    List<EmailNotification> findFailedEmailsForRetry(Pageable pageable);

    // BREAKPOINT: Debug email status counts (for dashboard stats)
    @Query("SELECT COUNT(en) FROM EmailNotification en WHERE en.status = :status")
    long countByStatus(String status);

    // BREAKPOINT: Debug email status distribution (GROUP BY aggregation)
    // Watch SQL: SELECT status, COUNT(*) FROM email_notifications GROUP BY status
    @Query("SELECT en.status, COUNT(en) FROM EmailNotification en GROUP BY en.status")
    List<Object[]> getStatusCounts();
}
