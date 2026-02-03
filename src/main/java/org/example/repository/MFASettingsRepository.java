package org.example.repository;

import org.example.entity.MFASettings;
import org.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for MFA Settings entity operations.
 */
@Repository
public interface MFASettingsRepository extends JpaRepository<MFASettings, Long> {

    /**
     * Find MFA settings by user.
     *
     * @param user the user
     * @return Optional containing MFA settings if found
     */
    Optional<MFASettings> findByUser(User user);

    /**
     * Find MFA settings by user ID.
     *
     * @param userId the user ID
     * @return Optional containing MFA settings if found
     */
    Optional<MFASettings> findByUserId(Long userId);

    /**
     * Check if user has MFA settings.
     *
     * @param user the user
     * @return true if MFA settings exist
     */
    boolean existsByUser(User user);

    /**
     * Delete MFA settings by user.
     *
     * @param user the user
     */
    void deleteByUser(User user);
}
