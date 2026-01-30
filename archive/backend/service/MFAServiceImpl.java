package org.example.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.*;
import org.example.entity.AuditLog;
import org.example.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MFAServiceImpl implements MFAService {

    private final MFASettingsRepository mfaSettingsRepository;
    private final BackupCodeRepository backupCodeRepository;
    private final TrustedDeviceRepository trustedDeviceRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final WebSocketEventPublisher webSocketEventPublisher;

    @Value("${mfa.totp.issuer}")
    private String issuer;

    @Value("${mfa.totp.window}")
    private int window;

    @Value("${mfa.backup.code.count}")
    private int backupCodeCount;

    @Value("${mfa.backup.code.length}")
    private int backupCodeLength;

    @Value("${mfa.trusted.device.days}")
    private int trustedDeviceDays;

    private final GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();

    @Override
    @Transactional
    public Map<String, Object> setupMFA(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Generate TOTP secret
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        String secret = key.getKey();

        // Create or update MFA settings
        MFASettings settings = mfaSettingsRepository.findByUser(user)
                .orElse(new MFASettings());

        settings.setUser(user);
        settings.setTotpSecret(secret);
        settings.setIsEnabled(false); // Not enabled until verified
        settings.setBackupCodesRemaining(0); // Will be generated after verification

        mfaSettingsRepository.save(settings);

        // Generate QR code URL
        String qrCodeUrl = GoogleAuthenticatorQRGenerator.getOtpAuthURL(
                issuer,
                user.getUsername(),
                key
        );

        // Log audit event
        logAuditEvent(AuditEventType.MFA_SETUP_INITIATED, user.getId(), user.getUsername(), user.getRole().name(),
                "MFA setup initiated for user: " + user.getUsername());

        Map<String, Object> response = new HashMap<>();
        response.put("secret", secret);
        response.put("qrCodeUrl", qrCodeUrl);
        response.put("issuer", issuer);
        response.put("username", user.getUsername());

        return response;
    }

    @Override
    @Transactional
    public boolean verifyAndEnableMFA(Long userId, String totpCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        MFASettings settings = mfaSettingsRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("MFA not set up"));

        // Verify TOTP code
        boolean isValid = googleAuthenticator.authorize(settings.getTotpSecret(), Integer.parseInt(totpCode), window);

        if (isValid) {
            // Enable MFA
            settings.setIsEnabled(true);
            settings.setVerifiedAt(LocalDateTime.now());
            mfaSettingsRepository.save(settings);

            // Update user entity
            user.setMfaEnabled(true);
            userRepository.save(user);

            // Generate backup codes
            List<String> backupCodes = generateBackupCodesInternal(user, settings);

            // Log audit event
            logAuditEvent(AuditEventType.MFA_ENABLED, user.getId(), user.getUsername(), user.getRole().name(),
                    "MFA enabled successfully");

            log.info("MFA enabled for user: {}", user.getUsername());
            return true;
        }

        // Log failed verification
        logAuditEvent(AuditEventType.MFA_VERIFICATION_FAILURE, user.getId(), user.getUsername(), user.getRole().name(),
                "Failed MFA verification during setup");

        return false;
    }

    @Override
    public boolean verifyTOTP(Long userId, String totpCode) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        MFASettings settings = mfaSettingsRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("MFA not enabled"));

        if (!settings.getIsEnabled()) {
            return false;
        }

        try {
            int code = Integer.parseInt(totpCode);
            boolean isValid = googleAuthenticator.authorize(settings.getTotpSecret(), code, window);

            if (isValid) {
                logAuditEvent(AuditEventType.MFA_VERIFICATION_SUCCESS, user.getId(), user.getUsername(), user.getRole().name(),
                        "MFA verification successful");
            } else {
                logAuditEvent(AuditEventType.MFA_VERIFICATION_FAILURE, user.getId(), user.getUsername(), user.getRole().name(),
                        "MFA verification failed");
            }

            return isValid;
        } catch (NumberFormatException e) {
            log.error("Invalid TOTP code format", e);
            return false;
        }
    }

    @Override
    @Transactional
    public boolean verifyBackupCode(Long userId, String backupCode, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<BackupCode> codes = backupCodeRepository.findByUserAndIsUsedFalse(user);

        for (BackupCode code : codes) {
            if (passwordEncoder.matches(backupCode, code.getCodeHash())) {
                // Mark code as used
                code.setIsUsed(true);
                code.setUsedAt(LocalDateTime.now());
                code.setUsedFromIp(ipAddress);
                backupCodeRepository.save(code);

                // Update remaining count
                MFASettings settings = mfaSettingsRepository.findByUser(user)
                        .orElseThrow(() -> new RuntimeException("MFA settings not found"));
                settings.setBackupCodesRemaining(settings.getBackupCodesRemaining() - 1);
                mfaSettingsRepository.save(settings);

                // Log audit event
                logAuditEvent(AuditEventType.BACKUP_CODE_USED, user.getId(), user.getUsername(), user.getRole().name(),
                        "Backup code used for login");

                log.info("Backup code used for user: {}", user.getUsername());
                return true;
            }
        }

        return false;
    }

    @Override
    @Transactional
    public List<String> regenerateBackupCodes(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        MFASettings settings = mfaSettingsRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("MFA not enabled"));

        // Delete old backup codes
        backupCodeRepository.deleteByUserId(userId);

        // Generate new backup codes
        List<String> newCodes = generateBackupCodesInternal(user, settings);

        // Log audit event
        logAuditEvent(AuditEventType.BACKUP_CODES_REGENERATED, user.getId(), user.getUsername(), user.getRole().name(),
                "Backup codes regenerated");

        return newCodes;
    }

    @Override
    @Transactional
    public void trustDevice(Long userId, String deviceFingerprint, String deviceName, String userAgent, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Hash the device fingerprint
        String fingerprintHash = passwordEncoder.encode(deviceFingerprint);

        TrustedDevice device = new TrustedDevice();
        device.setUser(user);
        device.setDeviceFingerprintHash(fingerprintHash);
        device.setDeviceName(deviceName);
        device.setUserAgent(userAgent);
        device.setIpAddress(ipAddress);
        device.setIsActive(true);
        device.setExpiresAt(LocalDateTime.now().plusDays(trustedDeviceDays));
        device.setLastUsedAt(LocalDateTime.now());

        trustedDeviceRepository.save(device);

        // Log audit event
        logAuditEvent(AuditEventType.DEVICE_TRUSTED, user.getId(), user.getUsername(), user.getRole().name(),
                "Device trusted: " + deviceName);

        log.info("Device trusted for user: {}, device: {}", user.getUsername(), deviceName);
    }

    @Override
    public boolean isDeviceTrusted(Long userId, String deviceFingerprint) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<TrustedDevice> devices = trustedDeviceRepository.findByUserAndIsActiveTrue(user);

        for (TrustedDevice device : devices) {
            if (passwordEncoder.matches(deviceFingerprint, device.getDeviceFingerprintHash())) {
                // Check if device is still valid (not expired)
                if (device.getExpiresAt().isAfter(LocalDateTime.now())) {
                    // Update last used time
                    device.setLastUsedAt(LocalDateTime.now());
                    trustedDeviceRepository.save(device);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    @Transactional
    public void disableMFA(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        MFASettings settings = mfaSettingsRepository.findByUser(user)
                .orElseThrow(() -> new RuntimeException("MFA not enabled"));

        settings.setIsEnabled(false);
        mfaSettingsRepository.save(settings);

        // Update user entity
        user.setMfaEnabled(false);
        userRepository.save(user);

        // Delete backup codes
        backupCodeRepository.deleteByUserId(userId);

        // Log audit event
        logAuditEvent(AuditEventType.MFA_DISABLED, user.getId(), user.getUsername(), user.getRole().name(),
                "MFA disabled");

        log.info("MFA disabled for user: {}", user.getUsername());
    }

    @Override
    public List<Map<String, Object>> getTrustedDevices(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<TrustedDevice> devices = trustedDeviceRepository.findByUserAndIsActiveTrue(user);

        return devices.stream().map(device -> {
            Map<String, Object> deviceMap = new HashMap<>();
            deviceMap.put("id", device.getId());
            deviceMap.put("deviceName", device.getDeviceName());
            deviceMap.put("userAgent", device.getUserAgent());
            deviceMap.put("ipAddress", device.getIpAddress());
            deviceMap.put("createdAt", device.getCreatedAt());
            deviceMap.put("expiresAt", device.getExpiresAt());
            deviceMap.put("lastUsedAt", device.getLastUsedAt());
            return deviceMap;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void removeTrustedDevice(Long deviceId) {
        TrustedDevice device = trustedDeviceRepository.findById(deviceId)
                .orElseThrow(() -> new RuntimeException("Device not found"));

        device.setIsActive(false);
        trustedDeviceRepository.save(device);

        // Log audit event
        logAuditEvent(AuditEventType.DEVICE_UNTRUSTED, device.getUser().getId(), device.getUser().getUsername(),
                device.getUser().getRole().name(), "Device untrusted: " + device.getDeviceName());

        log.info("Device untrusted: {}", deviceId);
    }

    @Override
    public Map<String, Object> getMFAStatus(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<MFASettings> settingsOpt = mfaSettingsRepository.findByUser(user);

        Map<String, Object> status = new HashMap<>();
        if (settingsOpt.isPresent()) {
            MFASettings settings = settingsOpt.get();
            status.put("enabled", settings.getIsEnabled());
            status.put("verifiedAt", settings.getVerifiedAt());
            status.put("backupCodesRemaining", settings.getBackupCodesRemaining());
            status.put("backupCodesGeneratedAt", settings.getBackupCodesGeneratedAt());
        } else {
            status.put("enabled", false);
            status.put("backupCodesRemaining", 0);
        }

        // Count trusted devices
        long trustedDeviceCount = trustedDeviceRepository.findByUserAndIsActiveTrue(user).size();
        status.put("trustedDeviceCount", trustedDeviceCount);

        return status;
    }

    // Private helper methods

    private void logAuditEvent(AuditEventType eventType, Long userId, String username, String userRole, String action) {
        AuditLog auditLog = AuditLog.builder()
                .userId(userId)
                .username(username)
                .userRole(userRole)
                .eventType(eventType)
                .action(action)
                .status("SUCCESS")
                .requestId(java.util.UUID.randomUUID().toString())
                .build();
        auditLogService.log(auditLog);
    }

    private List<String> generateBackupCodesInternal(User user, MFASettings settings) {
        List<String> plainCodes = new ArrayList<>();
        SecureRandom random = new SecureRandom();

        for (int i = 0; i < backupCodeCount; i++) {
            String code = generateRandomCode(backupCodeLength, random);
            plainCodes.add(code);

            BackupCode backupCode = new BackupCode();
            backupCode.setUser(user);
            backupCode.setCodeHash(passwordEncoder.encode(code));
            backupCode.setIsUsed(false);
            backupCodeRepository.save(backupCode);
        }

        settings.setBackupCodesGeneratedAt(LocalDateTime.now());
        settings.setBackupCodesRemaining(backupCodeCount);
        mfaSettingsRepository.save(settings);

        return plainCodes;
    }

    private String generateRandomCode(int length, SecureRandom random) {
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < length; i++) {
            code.append(random.nextInt(10));
        }
        return code.toString();
    }

    // Scheduled task to clean up expired trusted devices
    @Scheduled(cron = "0 0 2 * * *") // Daily at 2 AM
    @Transactional
    public void cleanupExpiredTrustedDevices() {
        int deactivated = trustedDeviceRepository.deactivateExpiredDevices(LocalDateTime.now());
        log.info("Deactivated {} expired trusted devices", deactivated);

        // Delete old expired devices (older than 90 days)
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(90);
        int deleted = trustedDeviceRepository.deleteOldExpiredDevices(cutoffDate);
        log.info("Deleted {} old expired trusted devices", deleted);
    }
}
