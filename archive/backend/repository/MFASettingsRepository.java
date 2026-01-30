package org.example.repository;

import org.example.entity.MFASettings;
import org.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for MFASettings entity operations.
 *
 * DEBUGGING GUIDE:
 * ----------------
 * To debug MFA configuration queries:
 * 1. Set breakpoints in RepositoryLoggingAspect.java
 * 2. Monitor queries during MFA setup and verification
 *
 * BREAKPOINT LOCATIONS:
 * - Line 24: findByUser() - Debug MFA settings retrieval during login
 * - Line 26: findByUserId() - Debug MFA status checks
 * - Line 28: existsByUser() - Debug MFA enabled/disabled checks
 */
@Repository
public interface MFASettingsRepository extends JpaRepository<MFASettings, Long> {

    // BREAKPOINT: Debug MFA settings lookup during login/verification
    Optional<MFASettings> findByUser(User user);

    // BREAKPOINT: Debug MFA configuration by user ID
    Optional<MFASettings> findByUserId(Long userId);

    // BREAKPOINT: Debug MFA enabled status checks
    boolean existsByUser(User user);
}
