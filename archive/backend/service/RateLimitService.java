package org.example.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

import java.util.Map;

public interface RateLimitService {

    /**
     * Check if request should be rate limited
     * @return Map with keys: allowed (boolean), limit (int), remaining (int), resetTime (long)
     */
    Map<String, Object> checkRateLimit(HttpServletRequest request, Authentication authentication);

    /**
     * Check if IP or user is whitelisted
     */
    boolean isWhitelisted(String ipAddress, String username);

    /**
     * Add IP to whitelist
     */
    void addToWhitelist(String ipAddress, String reason, String createdBy, Integer expiresInDays);

    /**
     * Add user to whitelist
     */
    void addUserToWhitelist(Long userId, String reason, String createdBy, Integer expiresInDays);

    /**
     * Remove from whitelist
     */
    void removeFromWhitelist(Long whitelistId);

    /**
     * Get all whitelisted entries
     */
    java.util.List<Map<String, Object>> getWhitelist();

    /**
     * Get rate limit statistics
     */
    Map<String, Object> getStatistics();

    /**
     * Get recent violations
     */
    java.util.List<Map<String, Object>> getRecentViolations(int limit);
}
