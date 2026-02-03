package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.LoginAttempt;
import org.example.entity.User;
import org.example.repository.LoginAttemptRepository;
import org.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Implementation of account locking and brute force protection service.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountLockServiceImpl implements AccountLockService {

    private final LoginAttemptRepository loginAttemptRepository;
    private final UserRepository userRepository;

    @Value("${app.security.account-lock.max-failed-attempts:5}")
    private int maxFailedAttempts;

    @Value("${app.security.account-lock.lockout-duration-minutes:30}")
    private int lockoutDurationMinutes;

    @Value("${app.security.account-lock.reset-time-minutes:15}")
    private int resetTimeMinutes;

    @Value("${app.security.ip-block.max-failed-attempts:10}")
    private int maxFailedAttemptsPerIp;

    @Value("${app.security.login-attempts.retention-days:90}")
    private int retentionDays;

    @Override
    @Transactional
    public void recordSuccessfulLogin(String username, String ipAddress, String userAgent) {
        log.debug("Recording successful login for user: {} from IP: {}", username, ipAddress);

        LoginAttempt attempt = new LoginAttempt();
        attempt.setUsername(username);
        attempt.setIpAddress(ipAddress);
        attempt.setUserAgent(userAgent);
        attempt.setSuccess(true);
        attempt.setFailureReason(null);

        loginAttemptRepository.save(attempt);

        // Reset failed attempts counter on successful login
        resetFailedAttempts(username);
    }

    @Override
    @Transactional
    public void recordFailedLogin(String username, String ipAddress, String userAgent, String failureReason) {
        log.warn("Recording failed login for user: {} from IP: {} - Reason: {}", username, ipAddress, failureReason);

        LoginAttempt attempt = new LoginAttempt();
        attempt.setUsername(username);
        attempt.setIpAddress(ipAddress);
        attempt.setUserAgent(userAgent);
        attempt.setSuccess(false);
        attempt.setFailureReason(failureReason);

        loginAttemptRepository.save(attempt);

        // Check if account should be locked
        if (shouldLockAccount(username)) {
            userRepository.findByUsername(username).ifPresent(user -> {
                lockAccount(user, "Too many failed login attempts", lockoutDurationMinutes);
                log.warn("Account locked for user: {} due to {} failed attempts", username, maxFailedAttempts);
            });
        }
    }

    @Override
    public boolean shouldLockAccount(String username) {
        long failedCount = getFailedAttemptCount(username);
        boolean shouldLock = failedCount >= maxFailedAttempts;

        if (shouldLock) {
            log.warn("User {} has {} failed login attempts (threshold: {})", username, failedCount, maxFailedAttempts);
        }

        return shouldLock;
    }

    @Override
    public boolean shouldBlockIp(String ipAddress) {
        long failedCount = getFailedAttemptCountByIp(ipAddress);
        boolean shouldBlock = failedCount >= maxFailedAttemptsPerIp;

        if (shouldBlock) {
            log.warn("IP {} has {} failed login attempts (threshold: {})", ipAddress, failedCount, maxFailedAttemptsPerIp);
        }

        return shouldBlock;
    }

    @Override
    @Transactional
    public void lockAccount(User user, String reason, int durationMinutes) {
        log.warn("Locking account for user: {} - Reason: {} - Duration: {} minutes",
                 user.getUsername(), reason, durationMinutes);

        user.setIsAccountLocked(true);
        user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(durationMinutes));
        user.setLockReason(reason);

        userRepository.save(user);

        log.info("Account locked until: {} for user: {}", user.getAccountLockedUntil(), user.getUsername());
    }

    @Override
    @Transactional
    public void unlockAccount(User user) {
        log.info("Unlocking account for user: {}", user.getUsername());

        user.setIsAccountLocked(false);
        user.setAccountLockedUntil(null);
        user.setLockReason(null);

        userRepository.save(user);

        log.info("Account unlocked for user: {}", user.getUsername());
    }

    @Override
    @Transactional
    public boolean checkAndAutoUnlock(User user) {
        if (user.getIsAccountLocked() && user.getAccountLockedUntil() != null) {
            if (LocalDateTime.now().isAfter(user.getAccountLockedUntil())) {
                log.info("Auto-unlocking expired lock for user: {}", user.getUsername());
                unlockAccount(user);
                return true;
            }
        }
        return false;
    }

    @Override
    public long getFailedAttemptCount(String username) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(resetTimeMinutes);
        return loginAttemptRepository.countFailedAttemptsByUsername(username, since);
    }

    @Override
    public long getFailedAttemptCountByIp(String ipAddress) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(resetTimeMinutes);
        return loginAttemptRepository.countFailedAttemptsByIp(ipAddress, since);
    }

    @Override
    public void resetFailedAttempts(String username) {
        // Failed attempts are automatically "reset" by time window query
        // No need to delete records - they age out naturally
        log.debug("Failed attempts will reset after {} minutes for user: {}", resetTimeMinutes, username);
    }

    @Override
    @Transactional
    public int cleanupOldAttempts() {
        log.info("Cleaning up login attempts older than {} days", retentionDays);

        LocalDateTime before = LocalDateTime.now().minusDays(retentionDays);
        int deleted = loginAttemptRepository.deleteOldAttempts(before);

        log.info("Deleted {} old login attempts", deleted);
        return deleted;
    }
}
