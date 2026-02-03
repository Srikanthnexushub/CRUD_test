package org.example.service;

import org.example.dto.PasswordResetRequest;
import org.example.dto.PasswordResetInitiateRequest;

/**
 * Password Reset Service Interface
 */
public interface PasswordResetService {

    /**
     * Initiate password reset process by sending email with reset link
     */
    void initiatePasswordReset(PasswordResetInitiateRequest request, String ipAddress, String userAgent);

    /**
     * Validate reset token
     */
    boolean validateResetToken(String token);

    /**
     * Reset password using token
     */
    void resetPassword(PasswordResetRequest request, String ipAddress);

    /**
     * Invalidate all tokens for a user (e.g., after password change)
     */
    void invalidateAllTokensForUser(Long userId);

    /**
     * Clean up expired tokens (scheduled task)
     */
    void cleanupExpiredTokens();
}
