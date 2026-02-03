package org.example.service;

import org.example.dto.MFASetupResponse;
import org.example.dto.TrustedDeviceResponse;
import org.example.entity.User;

import java.util.List;

/**
 * Service interface for Multi-Factor Authentication operations.
 */
public interface MFAService {

    /**
     * Generate MFA setup (secret, QR code, backup codes) for a user.
     * Does not enable MFA yet - requires verification with setupMFA().
     *
     * @param user the user
     * @return MFA setup response with QR code and backup codes
     */
    MFASetupResponse generateMFASetup(User user);

    /**
     * Enable MFA for a user after verifying the TOTP code.
     *
     * @param user the user
     * @param totpCode the 6-digit TOTP code to verify
     * @throws IllegalStateException if MFA setup not found or already verified
     * @throws IllegalArgumentException if TOTP code is invalid
     */
    void enableMFA(User user, String totpCode);

    /**
     * Disable MFA for a user.
     *
     * @param user the user
     */
    void disableMFA(User user);

    /**
     * Verify a TOTP code or backup code for a user.
     *
     * @param user the user
     * @param code the 6-digit TOTP code or 8-character backup code
     * @return true if code is valid
     */
    boolean verifyCode(User user, String code);

    /**
     * Verify a TOTP code (not backup code).
     *
     * @param user the user
     * @param totpCode the 6-digit TOTP code
     * @return true if TOTP code is valid
     */
    boolean verifyTOTPCode(User user, String totpCode);

    /**
     * Verify a backup code and mark it as used.
     *
     * @param user the user
     * @param backupCode the 8-character backup code
     * @return true if backup code is valid and not used
     */
    boolean verifyAndUseBackupCode(User user, String backupCode);

    /**
     * Generate new backup codes for a user (invalidates old ones).
     *
     * @param user the user
     * @return array of new backup codes
     */
    String[] regenerateBackupCodes(User user);

    /**
     * Trust a device for a user (skip MFA for 30 days).
     *
     * @param user the user
     * @param deviceIdentifier unique device identifier
     * @param deviceName human-readable device name
     * @param ipAddress IP address
     * @param userAgent user agent string
     */
    void trustDevice(User user, String deviceIdentifier, String deviceName, String ipAddress, String userAgent);

    /**
     * Check if a device is trusted for a user.
     *
     * @param user the user
     * @param deviceIdentifier unique device identifier
     * @return true if device is trusted and not expired
     */
    boolean isDeviceTrusted(User user, String deviceIdentifier);

    /**
     * Update last used timestamp for a trusted device.
     *
     * @param user the user
     * @param deviceIdentifier unique device identifier
     */
    void updateDeviceLastUsed(User user, String deviceIdentifier);

    /**
     * Get all trusted devices for a user.
     *
     * @param user the user
     * @return list of trusted devices
     */
    List<TrustedDeviceResponse> getTrustedDevices(User user);

    /**
     * Revoke a trusted device.
     *
     * @param user the user
     * @param deviceId device ID
     */
    void revokeTrustedDevice(User user, Long deviceId);

    /**
     * Revoke all trusted devices for a user.
     *
     * @param user the user
     */
    void revokeAllTrustedDevices(User user);

    /**
     * Clean up expired trusted devices (scheduled task).
     *
     * @return number of devices deleted
     */
    int cleanupExpiredDevices();

    /**
     * Generate a device identifier from request components.
     *
     * @param ipAddress IP address
     * @param userAgent user agent
     * @return unique device identifier (SHA-256 hash)
     */
    String generateDeviceIdentifier(String ipAddress, String userAgent);
}
