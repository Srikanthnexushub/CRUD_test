package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.PasswordResetInitiateRequest;
import org.example.dto.PasswordResetRequest;
import org.example.entity.PasswordResetToken;
import org.example.entity.User;
import org.example.enums.AuditAction;
import org.example.exception.InvalidCredentialsException;
import org.example.exception.ResourceNotFoundException;
import org.example.exception.ValidationException;
import org.example.repository.PasswordResetTokenRepository;
import org.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Password Reset Service Implementation
 * Handles password reset flow with token-based authentication
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final AuditLogService auditLogService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.security.password-reset.token-expiration-minutes:30}")
    private int tokenExpirationMinutes;

    @Value("${app.security.password-reset.max-requests-per-hour:3}")
    private int maxRequestsPerHour;

    @Override
    @Transactional
    public void initiatePasswordReset(PasswordResetInitiateRequest request, String ipAddress, String userAgent) {
        // Find user by email or username
        User user = userRepository.findByEmail(request.getEmail())
            .or(() -> userRepository.findByUsername(request.getEmail()))
            .orElse(null);

        // Don't reveal if user exists (security best practice)
        if (user == null) {
            log.warn("Password reset requested for non-existent email: {}", request.getEmail());
            // Still return success to prevent user enumeration
            return;
        }

        // Check if account is locked
        if (user.getIsAccountLocked()) {
            log.warn("Password reset requested for locked account: {}", user.getUsername());
            throw new ValidationException("Account is locked. Please contact support.");
        }

        // Rate limiting: Check recent token requests
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        long recentTokenCount = tokenRepository.countRecentTokensForUser(user, oneHourAgo);

        if (recentTokenCount >= maxRequestsPerHour) {
            log.warn("Too many password reset requests for user: {}", user.getUsername());
            auditLogService.log(
                AuditAction.PASSWORD_RESET_REQUEST_RATE_LIMITED,
                user.getId(),
                "PasswordResetToken",
                null,
                ipAddress,
                userAgent,
                "Too many password reset requests"
            );
            throw new ValidationException("Too many password reset requests. Please try again later.");
        }

        // Generate unique token
        String token = UUID.randomUUID().toString().replace("-", "");

        // Create token entity
        PasswordResetToken resetToken = PasswordResetToken.builder()
            .token(token)
            .user(user)
            .expiresAt(LocalDateTime.now().plusMinutes(tokenExpirationMinutes))
            .ipAddress(ipAddress)
            .userAgent(userAgent)
            .isUsed(false)
            .build();

        tokenRepository.save(resetToken);

        // Send reset email
        try {
            emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), token);
            log.info("Password reset email sent to user: {}", user.getUsername());
        } catch (Exception e) {
            log.error("Failed to send password reset email to user: {}", user.getUsername(), e);
            // Don't throw exception - token is still valid
        }

        // Audit log
        auditLogService.log(
            AuditAction.PASSWORD_RESET_REQUESTED,
            user.getId(),
            "PasswordResetToken",
            resetToken.getId(),
            ipAddress,
            userAgent,
            "Password reset token created"
        );
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validateResetToken(String token) {
        return tokenRepository.findValidToken(token, LocalDateTime.now()).isPresent();
    }

    @Override
    @Transactional
    public void resetPassword(PasswordResetRequest request, String ipAddress) {
        // Validate token
        PasswordResetToken resetToken = tokenRepository.findValidToken(request.getToken(), LocalDateTime.now())
            .orElseThrow(() -> new InvalidCredentialsException("Invalid or expired reset token"));

        User user = resetToken.getUser();

        // Validate password
        if (request.getNewPassword().length() < 8) {
            throw new ValidationException("Password must be at least 8 characters long");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new ValidationException("Passwords do not match");
        }

        // Additional password validation (reuse from password policy)
        validatePasswordComplexity(request.getNewPassword());

        // Check if new password is same as old password
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new ValidationException("New password must be different from the current password");
        }

        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);

        // Mark token as used
        resetToken.setIsUsed(true);
        resetToken.setUsedAt(LocalDateTime.now());
        tokenRepository.save(resetToken);

        // Invalidate all other tokens for this user
        invalidateAllTokensForUser(user.getId());

        // Send confirmation email
        try {
            emailService.sendPasswordChangedEmail(user.getEmail(), user.getUsername());
        } catch (Exception e) {
            log.error("Failed to send password changed email to user: {}", user.getUsername(), e);
        }

        // Audit log
        auditLogService.log(
            AuditAction.PASSWORD_RESET_COMPLETED,
            user.getId(),
            "User",
            user.getId(),
            ipAddress,
            null,
            "Password successfully reset via email token"
        );

        log.info("Password reset completed for user: {}", user.getUsername());
    }

    @Override
    @Transactional
    public void invalidateAllTokensForUser(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        tokenRepository.invalidateAllTokensForUser(user);
        log.info("All password reset tokens invalidated for user: {}", user.getUsername());
    }

    @Override
    @Transactional
    @Scheduled(cron = "${app.scheduled.cleanup-password-reset-tokens:0 0 3 * * *}") // 3 AM daily
    public void cleanupExpiredTokens() {
        try {
            tokenRepository.deleteExpiredTokens(LocalDateTime.now());
            log.info("Expired password reset tokens cleaned up");
        } catch (Exception e) {
            log.error("Failed to cleanup expired password reset tokens", e);
        }
    }

    /**
     * Validate password complexity requirements
     */
    private void validatePasswordComplexity(String password) {
        if (password.length() < 8) {
            throw new ValidationException("Password must be at least 8 characters long");
        }

        boolean hasUpperCase = password.matches(".*[A-Z].*");
        boolean hasLowerCase = password.matches(".*[a-z].*");
        boolean hasDigit = password.matches(".*\\d.*");
        boolean hasSpecial = password.matches(".*[!@#$%^&*(),.?\":{}|<>].*");

        if (!hasUpperCase) {
            throw new ValidationException("Password must contain at least one uppercase letter");
        }

        if (!hasLowerCase) {
            throw new ValidationException("Password must contain at least one lowercase letter");
        }

        if (!hasDigit) {
            throw new ValidationException("Password must contain at least one digit");
        }

        // Optional: Require special character
        // if (!hasSpecial) {
        //     throw new ValidationException("Password must contain at least one special character");
        // }
    }
}
