package org.example.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.UserSession;
import org.example.repository.UserSessionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing user sessions with advanced intelligence
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final UserSessionRepository sessionRepository;
    private final AuditLogService auditLogService;
    private static final int MAX_CONCURRENT_SESSIONS = 5;
    private static final int SESSION_TIMEOUT_HOURS = 24;

    /**
     * Create a new session when user logs in
     */
    @Transactional
    public UserSession createSession(Long userId, String username, HttpServletRequest request,
                                     String deviceFingerprint) {
        String sessionToken = UUID.randomUUID().toString();

        // Parse user agent for device information
        String userAgent = request.getHeader("User-Agent");
        DeviceInfo deviceInfo = parseUserAgent(userAgent);

        // Get location from IP
        String ipAddress = getClientIpAddress(request);
        LocationInfo locationInfo = getLocationFromIp(ipAddress);

        // Check for suspicious activity
        SuspiciousActivity suspiciousActivity = detectSuspiciousActivity(userId, deviceFingerprint,
            ipAddress, locationInfo);

        UserSession session = UserSession.builder()
            .userId(userId)
            .username(username)
            .sessionToken(sessionToken)
            .deviceFingerprint(deviceFingerprint)
            .deviceType(deviceInfo.deviceType)
            .browser(deviceInfo.browser)
            .browserVersion(deviceInfo.browserVersion)
            .operatingSystem(deviceInfo.os)
            .osVersion(deviceInfo.osVersion)
            .userAgent(userAgent)
            .ipAddress(ipAddress)
            .country(locationInfo.country)
            .city(locationInfo.city)
            .latitude(locationInfo.latitude)
            .longitude(locationInfo.longitude)
            .timezone(locationInfo.timezone)
            .isActive(true)
            .lastActivity(LocalDateTime.now())
            .expiresAt(LocalDateTime.now().plusHours(SESSION_TIMEOUT_HOURS))
            .riskScore(suspiciousActivity.riskScore)
            .isSuspicious(suspiciousActivity.isSuspicious)
            .suspiciousReasons(suspiciousActivity.reasons)
            .build();

        UserSession savedSession = sessionRepository.save(session);

        // Check concurrent sessions limit
        manageConcurrentSessions(userId);

        // Log suspicious activity if detected
        if (suspiciousActivity.isSuspicious) {
            auditLogService.logSuspiciousActivity(username,
                "Session created with suspicious indicators",
                suspiciousActivity.reasons);
        }

        log.info("Session created for user {} from {} ({})", username, ipAddress, locationInfo.city);
        return savedSession;
    }

    /**
     * Update session activity
     */
    @Transactional
    public void updateSessionActivity(String sessionToken) {
        sessionRepository.findBySessionTokenAndIsActiveTrue(sessionToken)
            .ifPresent(session -> {
                session.setLastActivity(LocalDateTime.now());
                sessionRepository.save(session);
            });
    }

    /**
     * Terminate a session
     */
    @Transactional
    public void terminateSession(String sessionToken, String reason) {
        sessionRepository.findBySessionTokenAndIsActiveTrue(sessionToken)
            .ifPresent(session -> {
                session.setIsActive(false);
                session.setTerminatedAt(LocalDateTime.now());
                session.setTerminationReason(reason);
                sessionRepository.save(session);
                log.info("Session terminated for user {} - Reason: {}", session.getUsername(), reason);
            });
    }

    /**
     * Get active sessions for a user
     */
    public List<UserSession> getActiveSessions(Long userId) {
        return sessionRepository.findByUserIdAndIsActiveTrue(userId);
    }

    /**
     * Get session count for a user
     */
    public long getActiveSessionCount(Long userId) {
        return sessionRepository.countByUserIdAndIsActiveTrue(userId);
    }

    /**
     * Get all sessions for a user
     */
    public Page<UserSession> getUserSessions(Long userId, Pageable pageable) {
        return sessionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Force terminate all sessions for a user
     */
    @Transactional
    public void terminateAllUserSessions(Long userId, String reason) {
        List<UserSession> activeSessions = sessionRepository.findByUserIdAndIsActiveTrue(userId);
        activeSessions.forEach(session -> {
            session.setIsActive(false);
            session.setTerminatedAt(LocalDateTime.now());
            session.setTerminationReason(reason);
        });
        sessionRepository.saveAll(activeSessions);
        log.info("Terminated {} active sessions for user ID {}", activeSessions.size(), userId);
    }

    /**
     * Scheduled task to expire old sessions
     */
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    @Transactional
    public void expireOldSessions() {
        List<UserSession> expiredSessions = sessionRepository.findExpiredSessions(LocalDateTime.now());
        expiredSessions.forEach(session -> {
            session.setIsActive(false);
            session.setTerminatedAt(LocalDateTime.now());
            session.setTerminationReason("EXPIRED");
        });

        if (!expiredSessions.isEmpty()) {
            sessionRepository.saveAll(expiredSessions);
            log.info("Expired {} old sessions", expiredSessions.size());
        }
    }

    /**
     * Manage concurrent sessions - terminate oldest if limit exceeded
     */
    private void manageConcurrentSessions(Long userId) {
        List<UserSession> activeSessions = sessionRepository.findByUserIdAndIsActiveTrue(userId);

        if (activeSessions.size() > MAX_CONCURRENT_SESSIONS) {
            // Sort by creation time and terminate oldest
            activeSessions.stream()
                .sorted((s1, s2) -> s1.getCreatedAt().compareTo(s2.getCreatedAt()))
                .limit(activeSessions.size() - MAX_CONCURRENT_SESSIONS)
                .forEach(session -> {
                    session.setIsActive(false);
                    session.setTerminatedAt(LocalDateTime.now());
                    session.setTerminationReason("CONCURRENT_LIMIT");
                });

            sessionRepository.saveAll(activeSessions);
            log.warn("Terminated {} sessions for user {} due to concurrent session limit",
                activeSessions.size() - MAX_CONCURRENT_SESSIONS, userId);
        }
    }

    /**
     * Detect suspicious activity based on session patterns
     */
    private SuspiciousActivity detectSuspiciousActivity(Long userId, String deviceFingerprint,
                                                        String ipAddress, LocationInfo locationInfo) {
        int riskScore = 0;
        StringBuilder reasons = new StringBuilder();

        // Check for multiple active sessions from different locations
        List<UserSession> activeSessions = sessionRepository.findByUserIdAndIsActiveTrue(userId);
        if (!activeSessions.isEmpty()) {
            boolean differentCountry = activeSessions.stream()
                .anyMatch(s -> s.getCountry() != null &&
                    !s.getCountry().equals(locationInfo.country));

            if (differentCountry) {
                riskScore += 30;
                reasons.append("Different country login; ");
            }

            // Check for rapid location changes (impossible travel)
            Optional<UserSession> recentSession = activeSessions.stream()
                .filter(s -> s.getLastActivity().isAfter(LocalDateTime.now().minusHours(1)))
                .findFirst();

            if (recentSession.isPresent() && recentSession.get().getCountry() != null
                && !recentSession.get().getCountry().equals(locationInfo.country)) {
                riskScore += 40;
                reasons.append("Impossible travel detected; ");
            }
        }

        // Check for new device
        List<UserSession> deviceSessions = sessionRepository.findByDeviceFingerprintAndIsActiveTrue(deviceFingerprint);
        if (deviceSessions.isEmpty()) {
            riskScore += 10;
            reasons.append("New device; ");
        }

        // Check for known suspicious IPs (placeholder - would integrate with threat intelligence)
        if (ipAddress.startsWith("10.") || ipAddress.startsWith("192.168.")) {
            // Private IP ranges - low risk
        } else {
            // Could check against threat intelligence databases
        }

        boolean isSuspicious = riskScore >= 40;

        return new SuspiciousActivity(riskScore, isSuspicious,
            reasons.length() > 0 ? reasons.toString() : "None");
    }

    /**
     * Parse user agent string to extract device information
     */
    private DeviceInfo parseUserAgent(String userAgent) {
        if (userAgent == null) {
            return new DeviceInfo("Unknown", "Unknown", "Unknown", "Unknown", "Unknown");
        }

        String deviceType = "Desktop";
        String browser = "Unknown";
        String browserVersion = "Unknown";
        String os = "Unknown";
        String osVersion = "Unknown";

        // Detect device type
        if (userAgent.contains("Mobile")) {
            deviceType = "Mobile";
        } else if (userAgent.contains("Tablet")) {
            deviceType = "Tablet";
        }

        // Detect browser
        if (userAgent.contains("Firefox")) {
            browser = "Firefox";
        } else if (userAgent.contains("Chrome")) {
            browser = "Chrome";
        } else if (userAgent.contains("Safari")) {
            browser = "Safari";
        } else if (userAgent.contains("Edge")) {
            browser = "Edge";
        }

        // Detect OS
        if (userAgent.contains("Windows")) {
            os = "Windows";
        } else if (userAgent.contains("Mac OS")) {
            os = "macOS";
        } else if (userAgent.contains("Linux")) {
            os = "Linux";
        } else if (userAgent.contains("Android")) {
            os = "Android";
        } else if (userAgent.contains("iOS")) {
            os = "iOS";
        }

        return new DeviceInfo(deviceType, browser, browserVersion, os, osVersion);
    }

    /**
     * Get location information from IP address
     * In production, would use a geolocation API like MaxMind or ipapi
     */
    private LocationInfo getLocationFromIp(String ipAddress) {
        // Placeholder implementation
        // In production, integrate with GeoIP service
        return new LocationInfo("Unknown", "Unknown", null, null, "UTC");
    }

    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_CLIENT_IP"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }

    // Helper classes
    private record DeviceInfo(String deviceType, String browser, String browserVersion, String os, String osVersion) {}
    private record LocationInfo(String country, String city, Double latitude, Double longitude, String timezone) {}
    private record SuspiciousActivity(int riskScore, boolean isSuspicious, String reasons) {}
}
