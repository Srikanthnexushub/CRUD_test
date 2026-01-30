package org.example.repository;

import org.example.entity.NotificationPreference;
import org.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for NotificationPreference entity operations.
 *
 * DEBUGGING GUIDE:
 * ----------------
 * To debug notification preference queries:
 * 1. Set breakpoints in RepositoryLoggingAspect.java
 * 2. Monitor queries during preference updates and digest sending
 * 3. Watch for scheduled digest queries (daily at 8 AM, weekly on Monday)
 *
 * BREAKPOINT LOCATIONS:
 * - Line 29: findByUser() - Debug preference lookup by user entity
 * - Line 32: findByUserId() - Debug preference lookup by user ID
 * - Line 36: findAllWithDailyDigestEnabled() - Debug daily digest recipients (runs daily 8 AM)
 * - Line 40: findAllWithWeeklyDigestEnabled() - Debug weekly digest recipients (runs Monday 8 AM)
 */
@Repository
public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    // BREAKPOINT: Debug preference lookup by user entity
    Optional<NotificationPreference> findByUser(User user);

    // BREAKPOINT: Debug preference lookup by user ID (before sending notifications)
    Optional<NotificationPreference> findByUserId(Long userId);

    // BREAKPOINT: Debug daily digest recipients (scheduled task, runs daily at 8 AM)
    // Watch SQL: WHERE daily_digest_enabled = true
    @Query("SELECT np FROM NotificationPreference np WHERE np.dailyDigestEnabled = true")
    List<NotificationPreference> findAllWithDailyDigestEnabled();

    // BREAKPOINT: Debug weekly digest recipients (scheduled task, runs Monday at 8 AM)
    // Watch SQL: WHERE weekly_digest_enabled = true
    @Query("SELECT np FROM NotificationPreference np WHERE np.weeklyDigestEnabled = true")
    List<NotificationPreference> findAllWithWeeklyDigestEnabled();
}
