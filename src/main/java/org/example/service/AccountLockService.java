package org.example.service;

import org.example.entity.User;

/**
 * Service interface for account locking and brute force protection.
 */
public interface AccountLockService {

    /**
     * Record a successful login attempt.
     *
     * @param username the username
     * @param ipAddress the IP address
     * @param userAgent the user agent
     */
    void recordSuccessfulLogin(String username, String ipAddress, String userAgent);

    /**
     * Record a failed login attempt.
     *
     * @param username the username
     * @param ipAddress the IP address
     * @param userAgent the user agent
     * @param failureReason the reason for failure
     */
    void recordFailedLogin(String username, String ipAddress, String userAgent, String failureReason);

    /**
     * Check if user should be locked due to too many failed attempts.
     *
     * @param username the username
     * @return true if user should be locked
     */
    boolean shouldLockAccount(String username);

    /**
     * Check if IP address should be blocked due to too many failed attempts.
     *
     * @param ipAddress the IP address
     * @return true if IP should be blocked
     */
    boolean shouldBlockIp(String ipAddress);

    /**
     * Lock user account.
     *
     * @param user the user to lock
     * @param reason the reason for locking
     * @param durationMinutes lock duration in minutes
     */
    void lockAccount(User user, String reason, int durationMinutes);

    /**
     * Unlock user account.
     *
     * @param user the user to unlock
     */
    void unlockAccount(User user);

    /**
     * Check if account lock has expired and auto-unlock if needed.
     *
     * @param user the user to check
     * @return true if account was unlocked
     */
    boolean checkAndAutoUnlock(User user);

    /**
     * Get count of failed login attempts for username within time window.
     *
     * @param username the username
     * @return count of failed attempts
     */
    long getFailedAttemptCount(String username);

    /**
     * Get count of failed login attempts from IP within time window.
     *
     * @param ipAddress the IP address
     * @return count of failed attempts
     */
    long getFailedAttemptCountByIp(String ipAddress);

    /**
     * Reset failed login attempts for username (after successful login).
     *
     * @param username the username
     */
    void resetFailedAttempts(String username);

    /**
     * Clean up old login attempts (scheduled task).
     *
     * @return number of attempts deleted
     */
    int cleanupOldAttempts();
}
