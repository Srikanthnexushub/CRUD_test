package org.example.service;

import org.example.entity.User;

import java.util.List;
import java.util.Map;

public interface MFAService {

    /**
     * Setup MFA for a user - generates secret and QR code
     */
    Map<String, Object> setupMFA(Long userId);

    /**
     * Verify TOTP code and enable MFA
     */
    boolean verifyAndEnableMFA(Long userId, String totpCode);

    /**
     * Verify TOTP code for login
     */
    boolean verifyTOTP(Long userId, String totpCode);

    /**
     * Verify backup code for login
     */
    boolean verifyBackupCode(Long userId, String backupCode, String ipAddress);

    /**
     * Generate new backup codes
     */
    List<String> regenerateBackupCodes(Long userId);

    /**
     * Trust a device for 30 days
     */
    void trustDevice(Long userId, String deviceFingerprint, String deviceName, String userAgent, String ipAddress);

    /**
     * Check if device is trusted
     */
    boolean isDeviceTrusted(Long userId, String deviceFingerprint);

    /**
     * Disable MFA for a user
     */
    void disableMFA(Long userId);

    /**
     * Get trusted devices for a user
     */
    List<Map<String, Object>> getTrustedDevices(Long userId);

    /**
     * Remove a trusted device
     */
    void removeTrustedDevice(Long deviceId);

    /**
     * Get MFA status for a user
     */
    Map<String, Object> getMFAStatus(Long userId);
}
