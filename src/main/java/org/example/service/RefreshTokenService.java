package org.example.service;

import jakarta.servlet.http.HttpServletRequest;
import org.example.dto.LoginResponse;
import org.example.entity.RefreshToken;
import org.example.entity.User;

import java.util.List;

/**
 * Service interface for JWT refresh token operations.
 */
public interface RefreshTokenService {

    /**
     * Create a new refresh token for a user.
     *
     * @param user the user
     * @param request HTTP request for IP and user agent
     * @return created refresh token
     */
    RefreshToken createRefreshToken(User user, HttpServletRequest request);

    /**
     * Verify and get refresh token by token string.
     *
     * @param token the token string
     * @return refresh token if valid
     * @throws IllegalArgumentException if token is invalid, expired, or revoked
     */
    RefreshToken verifyRefreshToken(String token);

    /**
     * Refresh access token using refresh token (with token rotation).
     *
     * @param refreshToken the refresh token string
     * @param request HTTP request
     * @return new login response with new access token and new refresh token
     * @throws IllegalArgumentException if refresh token is invalid
     */
    LoginResponse refreshAccessToken(String refreshToken, HttpServletRequest request);

    /**
     * Revoke a refresh token.
     *
     * @param token the token string
     */
    void revokeToken(String token);

    /**
     * Revoke all refresh tokens for a user.
     *
     * @param user the user
     * @return number of tokens revoked
     */
    int revokeAllUserTokens(User user);

    /**
     * Get all active refresh tokens for a user.
     *
     * @param user the user
     * @return list of active tokens
     */
    List<RefreshToken> getActiveTokens(User user);

    /**
     * Get count of active tokens for a user.
     *
     * @param user the user
     * @return count of active tokens
     */
    long getActiveTokenCount(User user);

    /**
     * Clean up expired and old revoked tokens (scheduled task).
     *
     * @return number of tokens deleted
     */
    int cleanupExpiredTokens();
}
