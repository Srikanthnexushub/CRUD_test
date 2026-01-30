package org.example.repository;

import org.example.entity.TrustedDevice;
import org.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for TrustedDevice entity operations.
 *
 * DEBUGGING GUIDE:
 * ----------------
 * To debug trusted device queries:
 * 1. Set breakpoints in RepositoryLoggingAspect.java
 * 2. Monitor queries during MFA verification (trusted device checks)
 * 3. Watch for scheduled cleanup queries (runs daily)
 *
 * BREAKPOINT LOCATIONS:
 * - Line 32: findByUserAndIsActiveTrue() - Debug active trusted devices for user
 * - Line 35: findByUserAndDeviceFingerprintHashAndIsActiveTrue() - Debug device trust check during login
 * - Line 39: deactivateExpiredDevices() - **CRITICAL** Debug scheduled cleanup (@Modifying UPDATE)
 * - Line 44: deleteOldExpiredDevices() - **CRITICAL** Debug old device deletion (@Modifying DELETE)
 */
@Repository
public interface TrustedDeviceRepository extends JpaRepository<TrustedDevice, Long> {

    // BREAKPOINT: Debug active trusted devices list (user settings page)
    List<TrustedDevice> findByUserAndIsActiveTrue(User user);

    // BREAKPOINT: Debug device trust verification during MFA login
    // Watch SQL: WHERE user_id = ? AND device_fingerprint_hash = ? AND is_active = true
    Optional<TrustedDevice> findByUserAndDeviceFingerprintHashAndIsActiveTrue(User user, String deviceFingerprintHash);

    // BREAKPOINT: **CRITICAL** Debug scheduled device expiration (UPDATE query, runs daily)
    // Watch SQL: UPDATE trusted_devices SET is_active = false WHERE expires_at < ?
    @Modifying
    @Query("UPDATE TrustedDevice td SET td.isActive = false WHERE td.expiresAt < :now")
    int deactivateExpiredDevices(LocalDateTime now);

    // BREAKPOINT: **CRITICAL** Debug old device cleanup (DELETE query, runs daily)
    // Watch SQL: DELETE FROM trusted_devices WHERE expires_at < ?
    @Modifying
    @Query("DELETE FROM TrustedDevice td WHERE td.expiresAt < :cutoffDate")
    int deleteOldExpiredDevices(LocalDateTime cutoffDate);
}
