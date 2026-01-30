package org.example.repository;

import org.example.entity.BackupCode;
import org.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for BackupCode entity operations.
 *
 * DEBUGGING GUIDE:
 * ----------------
 * To debug MFA backup code queries:
 * 1. Set breakpoints in RepositoryLoggingAspect.java
 * 2. Monitor queries during backup code generation and verification
 *
 * BREAKPOINT LOCATIONS:
 * - Line 28: findByUserAndIsUsedFalse() - Debug unused backup codes retrieval
 * - Line 31: findByUserId() - Debug all backup codes for user
 * - Line 35: deleteByUserId() - **CRITICAL** Debug backup code deletion (@Modifying query)
 * - Line 38: countByUserAndIsUsedFalse() - Debug remaining backup codes count
 */
@Repository
public interface BackupCodeRepository extends JpaRepository<BackupCode, Long> {

    // BREAKPOINT: Debug unused backup codes retrieval during verification
    List<BackupCode> findByUserAndIsUsedFalse(User user);

    // BREAKPOINT: Debug all backup codes for user
    List<BackupCode> findByUserId(Long userId);

    // BREAKPOINT: **CRITICAL** Debug backup code deletion (UPDATE query)
    // Watch SQL: DELETE FROM backup_codes WHERE user_id = ?
    @Modifying
    @Query("DELETE FROM BackupCode bc WHERE bc.user.id = :userId")
    void deleteByUserId(Long userId);

    // BREAKPOINT: Debug remaining backup codes count
    long countByUserAndIsUsedFalse(User user);
}
