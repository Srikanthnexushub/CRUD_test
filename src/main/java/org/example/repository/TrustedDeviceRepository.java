package org.example.repository;

import org.example.entity.TrustedDevice;
import org.example.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for Trusted Device entity operations.
 */
@Repository
public interface TrustedDeviceRepository extends JpaRepository<TrustedDevice, Long> {

    /**
     * Find trusted device by user and device identifier.
     *
     * @param user the user
     * @param deviceIdentifier the device identifier
     * @return Optional containing trusted device if found and not expired
     */
    @Query("SELECT td FROM TrustedDevice td WHERE td.user = :user " +
           "AND td.deviceIdentifier = :deviceIdentifier " +
           "AND td.trustedUntil > CURRENT_TIMESTAMP")
    Optional<TrustedDevice> findByUserAndDeviceIdentifier(
            @Param("user") User user,
            @Param("deviceIdentifier") String deviceIdentifier
    );

    /**
     * Find all trusted devices for a user (non-expired only).
     *
     * @param user the user
     * @return list of trusted devices
     */
    @Query("SELECT td FROM TrustedDevice td WHERE td.user = :user " +
           "AND td.trustedUntil > CURRENT_TIMESTAMP " +
           "ORDER BY td.lastUsedAt DESC")
    List<TrustedDevice> findActiveByUser(@Param("user") User user);

    /**
     * Find all trusted devices for a user (including expired).
     *
     * @param user the user
     * @return list of all trusted devices
     */
    List<TrustedDevice> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Delete expired trusted devices.
     *
     * @param now current timestamp
     * @return number of deleted records
     */
    @Modifying
    @Query("DELETE FROM TrustedDevice td WHERE td.trustedUntil < :now")
    int deleteExpiredDevices(@Param("now") LocalDateTime now);

    /**
     * Delete all trusted devices for a user.
     *
     * @param user the user
     */
    void deleteByUser(User user);

    /**
     * Check if device is trusted for user.
     *
     * @param user the user
     * @param deviceIdentifier the device identifier
     * @return true if device is trusted and not expired
     */
    @Query("SELECT CASE WHEN COUNT(td) > 0 THEN true ELSE false END " +
           "FROM TrustedDevice td WHERE td.user = :user " +
           "AND td.deviceIdentifier = :deviceIdentifier " +
           "AND td.trustedUntil > CURRENT_TIMESTAMP")
    boolean isDeviceTrusted(
            @Param("user") User user,
            @Param("deviceIdentifier") String deviceIdentifier
    );
}
