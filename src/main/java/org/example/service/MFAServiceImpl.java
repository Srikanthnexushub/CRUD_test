package org.example.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import com.warrenstrange.googleauth.GoogleAuthenticatorQRGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.MFASetupResponse;
import org.example.dto.TrustedDeviceResponse;
import org.example.entity.MFASettings;
import org.example.entity.TrustedDevice;
import org.example.entity.User;
import org.example.repository.MFASettingsRepository;
import org.example.repository.TrustedDeviceRepository;
import org.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of MFA service using Google Authenticator (TOTP).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MFAServiceImpl implements MFAService {

    private final MFASettingsRepository mfaSettingsRepository;
    private final TrustedDeviceRepository trustedDeviceRepository;
    private final UserRepository userRepository;
    private final GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();

    @Value("${app.name:CRUDTest}")
    private String appName;

    @Value("${mfa.backup-codes.count:10}")
    private int backupCodesCount;

    @Value("${mfa.trusted-device.duration-days:30}")
    private int trustedDeviceDurationDays;

    private static final int QR_CODE_SIZE = 300;
    private static final int BACKUP_CODE_LENGTH = 8;
    private static final String BACKUP_CODE_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";

    @Override
    @Transactional
    public MFASetupResponse generateMFASetup(User user) {
        log.info("Generating MFA setup for user: {}", user.getUsername());

        // Generate new TOTP secret
        GoogleAuthenticatorKey key = googleAuthenticator.createCredentials();
        String secret = key.getKey();

        // Generate OTPAuth URI for QR code
        String otpAuthUri = GoogleAuthenticatorQRGenerator.getOtpAuthTotpURL(
                appName,
                user.getUsername(),
                key
        );

        // Generate QR code image
        String qrCodeBase64 = generateQRCodeBase64(otpAuthUri);

        // Generate backup codes
        String[] backupCodes = generateBackupCodes();

        // Save or update MFA settings (not verified yet)
        MFASettings mfaSettings = mfaSettingsRepository.findByUser(user)
                .orElse(new MFASettings());

        mfaSettings.setUser(user);
        mfaSettings.setSecret(secret);
        mfaSettings.setBackupCodes(encryptBackupCodes(backupCodes));
        mfaSettings.setIsVerified(false);

        mfaSettingsRepository.save(mfaSettings);

        String instructions = "1. Scan the QR code with your authenticator app (Google Authenticator, Authy, etc.)\n" +
                "2. Enter the 6-digit code from your app to verify and enable MFA\n" +
                "3. Save the backup codes in a secure location - you can use them if you lose access to your authenticator app";

        return new MFASetupResponse(secret, otpAuthUri, qrCodeBase64, backupCodes, instructions);
    }

    @Override
    @Transactional
    public void enableMFA(User user, String totpCode) {
        log.info("Enabling MFA for user: {}", user.getUsername());

        MFASettings mfaSettings = mfaSettingsRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("MFA setup not found. Please call /mfa/setup first."));

        if (mfaSettings.getIsVerified()) {
            throw new IllegalStateException("MFA is already enabled for this user.");
        }

        // Verify TOTP code
        if (!verifyTOTPCode(user, totpCode)) {
            throw new IllegalArgumentException("Invalid TOTP code. Please try again.");
        }

        // Mark as verified
        mfaSettings.setIsVerified(true);
        mfaSettingsRepository.save(mfaSettings);

        // Update user entity
        user.setMfaEnabled(true);
        userRepository.save(user);

        log.info("MFA successfully enabled for user: {}", user.getUsername());
    }

    @Override
    @Transactional
    public void disableMFA(User user) {
        log.info("Disabling MFA for user: {}", user.getUsername());

        // Delete MFA settings
        mfaSettingsRepository.findByUser(user).ifPresent(mfaSettingsRepository::delete);

        // Delete all trusted devices
        trustedDeviceRepository.deleteByUser(user);

        // Update user entity
        user.setMfaEnabled(false);
        userRepository.save(user);

        log.info("MFA successfully disabled for user: {}", user.getUsername());
    }

    @Override
    public boolean verifyCode(User user, String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }

        // Try TOTP code first (6 digits)
        if (code.matches("^[0-9]{6}$")) {
            return verifyTOTPCode(user, code);
        }

        // Try backup code (8 characters)
        if (code.matches("^[A-Z0-9]{8}$")) {
            return verifyAndUseBackupCode(user, code);
        }

        return false;
    }

    @Override
    public boolean verifyTOTPCode(User user, String totpCode) {
        MFASettings mfaSettings = mfaSettingsRepository.findByUser(user).orElse(null);
        if (mfaSettings == null) {
            log.warn("No MFA settings found for user: {}", user.getUsername());
            return false;
        }

        try {
            int code = Integer.parseInt(totpCode);
            boolean isValid = googleAuthenticator.authorize(mfaSettings.getSecret(), code);

            if (isValid) {
                log.info("TOTP code verified for user: {}", user.getUsername());
            } else {
                log.warn("Invalid TOTP code for user: {}", user.getUsername());
            }

            return isValid;
        } catch (NumberFormatException e) {
            log.warn("Invalid TOTP code format for user: {}", user.getUsername());
            return false;
        }
    }

    @Override
    @Transactional
    public boolean verifyAndUseBackupCode(User user, String backupCode) {
        MFASettings mfaSettings = mfaSettingsRepository.findByUser(user).orElse(null);
        if (mfaSettings == null || mfaSettings.getBackupCodes() == null) {
            log.warn("No MFA settings or backup codes found for user: {}", user.getUsername());
            return false;
        }

        String[] encryptedCodes = mfaSettings.getBackupCodes();
        String[] decryptedCodes = decryptBackupCodes(encryptedCodes);

        // Check if backup code matches any unused code
        for (int i = 0; i < decryptedCodes.length; i++) {
            if (backupCode.equalsIgnoreCase(decryptedCodes[i])) {
                log.info("Backup code verified for user: {}", user.getUsername());

                // Mark code as used by replacing it with "USED"
                decryptedCodes[i] = "USED";
                mfaSettings.setBackupCodes(encryptBackupCodes(decryptedCodes));
                mfaSettingsRepository.save(mfaSettings);

                return true;
            }
        }

        log.warn("Invalid or already used backup code for user: {}", user.getUsername());
        return false;
    }

    @Override
    @Transactional
    public String[] regenerateBackupCodes(User user) {
        log.info("Regenerating backup codes for user: {}", user.getUsername());

        MFASettings mfaSettings = mfaSettingsRepository.findByUser(user)
                .orElseThrow(() -> new IllegalStateException("MFA is not enabled for this user."));

        String[] backupCodes = generateBackupCodes();
        mfaSettings.setBackupCodes(encryptBackupCodes(backupCodes));
        mfaSettingsRepository.save(mfaSettings);

        return backupCodes;
    }

    @Override
    @Transactional
    public void trustDevice(User user, String deviceIdentifier, String deviceName, String ipAddress, String userAgent) {
        log.info("Trusting device for user: {} - Device: {}", user.getUsername(), deviceName);

        TrustedDevice device = new TrustedDevice();
        device.setUser(user);
        device.setDeviceIdentifier(deviceIdentifier);
        device.setDeviceName(deviceName);
        device.setIpAddress(ipAddress);
        device.setUserAgent(userAgent);
        device.setTrustedUntil(LocalDateTime.now().plusDays(trustedDeviceDurationDays));
        device.setLastUsedAt(LocalDateTime.now());

        trustedDeviceRepository.save(device);
    }

    @Override
    public boolean isDeviceTrusted(User user, String deviceIdentifier) {
        return trustedDeviceRepository.isDeviceTrusted(user, deviceIdentifier);
    }

    @Override
    @Transactional
    public void updateDeviceLastUsed(User user, String deviceIdentifier) {
        trustedDeviceRepository.findByUserAndDeviceIdentifier(user, deviceIdentifier)
                .ifPresent(device -> {
                    device.setLastUsedAt(LocalDateTime.now());
                    trustedDeviceRepository.save(device);
                });
    }

    @Override
    public List<TrustedDeviceResponse> getTrustedDevices(User user) {
        return trustedDeviceRepository.findActiveByUser(user).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void revokeTrustedDevice(User user, Long deviceId) {
        log.info("Revoking trusted device ID {} for user: {}", deviceId, user.getUsername());

        trustedDeviceRepository.findById(deviceId)
                .filter(device -> device.getUser().getId().equals(user.getId()))
                .ifPresent(trustedDeviceRepository::delete);
    }

    @Override
    @Transactional
    public void revokeAllTrustedDevices(User user) {
        log.info("Revoking all trusted devices for user: {}", user.getUsername());
        trustedDeviceRepository.deleteByUser(user);
    }

    @Override
    @Transactional
    public int cleanupExpiredDevices() {
        log.info("Cleaning up expired trusted devices");
        int deleted = trustedDeviceRepository.deleteExpiredDevices(LocalDateTime.now());
        log.info("Deleted {} expired trusted devices", deleted);
        return deleted;
    }

    @Override
    public String generateDeviceIdentifier(String ipAddress, String userAgent) {
        try {
            String fingerprint = ipAddress + "|" + userAgent;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(fingerprint.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Error generating device identifier", e);
            return String.valueOf(System.currentTimeMillis());
        }
    }

    // ========================================
    // Private Helper Methods
    // ========================================

    private String generateQRCodeBase64(String otpAuthUri) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(
                    otpAuthUri,
                    BarcodeFormat.QR_CODE,
                    QR_CODE_SIZE,
                    QR_CODE_SIZE
            );

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
            byte[] qrCodeBytes = outputStream.toByteArray();

            return Base64.getEncoder().encodeToString(qrCodeBytes);
        } catch (Exception e) {
            log.error("Error generating QR code", e);
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    private String[] generateBackupCodes() {
        SecureRandom random = new SecureRandom();
        String[] codes = new String[backupCodesCount];

        for (int i = 0; i < backupCodesCount; i++) {
            StringBuilder code = new StringBuilder();
            for (int j = 0; j < BACKUP_CODE_LENGTH; j++) {
                int index = random.nextInt(BACKUP_CODE_CHARS.length());
                code.append(BACKUP_CODE_CHARS.charAt(index));
            }
            codes[i] = code.toString();
        }

        return codes;
    }

    private String[] encryptBackupCodes(String[] codes) {
        // In production, use proper encryption (AES-256)
        // For now, using Base64 encoding as placeholder
        return Arrays.stream(codes)
                .map(code -> Base64.getEncoder().encodeToString(code.getBytes(StandardCharsets.UTF_8)))
                .toArray(String[]::new);
    }

    private String[] decryptBackupCodes(String[] encryptedCodes) {
        // In production, use proper decryption (AES-256)
        // For now, using Base64 decoding as placeholder
        return Arrays.stream(encryptedCodes)
                .map(code -> new String(Base64.getDecoder().decode(code), StandardCharsets.UTF_8))
                .toArray(String[]::new);
    }

    private TrustedDeviceResponse mapToResponse(TrustedDevice device) {
        return new TrustedDeviceResponse(
                device.getId(),
                device.getDeviceName(),
                device.getIpAddress(),
                device.getCreatedAt(),
                device.getLastUsedAt(),
                device.getTrustedUntil()
        );
    }
}
