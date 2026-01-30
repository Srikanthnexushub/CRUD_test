package org.example.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.example.entity.*;
import org.example.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ThreatIntelligenceServiceImpl implements ThreatIntelligenceService {

    private final ThreatAssessmentRepository threatAssessmentRepository;
    private final IPReputationCacheRepository ipCacheRepository;
    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final AuditLogService auditLogService;
    private final EmailService emailService;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${threat.abuseipdb.api.key}")
    private String abuseIpDbApiKey;

    @Value("${threat.abuseipdb.api.url}")
    private String abuseIpDbUrl;

    @Value("${threat.ipapi.url}")
    private String ipApiUrl;

    @Value("${threat.score.threshold.high}")
    private int highRiskThreshold;

    @Value("${threat.score.threshold.critical}")
    private int criticalRiskThreshold;

    @Value("${threat.account.lock.minutes}")
    private int accountLockMinutes;

    @Value("${threat.cache.ttl.hours}")
    private int cacheTtlHours;

    @Value("${threat.enabled}")
    private boolean threatEnabled;

    @Override
    @Async
    @Transactional
    public void assessThreat(User user, String ipAddress, String deviceFingerprint, String userAgent) {
        if (!threatEnabled) {
            log.debug("Threat intelligence disabled, skipping assessment");
            return;
        }

        log.info("Starting threat assessment for user: {}, IP: {}", user.getUsername(), ipAddress);

        try {
            Map<String, Object> factors = new HashMap<>();

            // 1. Get IP reputation
            Map<String, Object> ipReputation = getIPReputation(ipAddress);
            factors.put("ipReputation", ipReputation);

            // 2. Check for location anomalies
            boolean locationAnomaly = checkLocationAnomaly(user, ipAddress, ipReputation);
            factors.put("locationAnomaly", locationAnomaly);

            // 3. Check for VPN/Proxy/Tor
            boolean isVpn = (Boolean) ipReputation.getOrDefault("isVpn", false);
            boolean isProxy = (Boolean) ipReputation.getOrDefault("isProxy", false);
            boolean isTor = (Boolean) ipReputation.getOrDefault("isTor", false);
            factors.put("isVpn", isVpn);
            factors.put("isProxy", isProxy);
            factors.put("isTor", isTor);

            // 4. Check recent failed login attempts
            long recentFailedLogins = threatAssessmentRepository.countRecentAssessments(user,
                    LocalDateTime.now().minusMinutes(30));
            factors.put("recentFailedLogins", recentFailedLogins);

            // 5. Check for device change
            boolean newDevice = checkNewDevice(user, deviceFingerprint);
            factors.put("newDevice", newDevice);

            // 6. Time-based factors (unusual login time)
            boolean unusualTime = checkUnusualLoginTime();
            factors.put("unusualTime", unusualTime);

            // Calculate risk score
            int riskScore = calculateThreatScore(factors);
            String riskLevel = determineRiskLevel(riskScore);

            // Create threat assessment
            ThreatAssessment assessment = new ThreatAssessment();
            assessment.setUser(user);
            assessment.setIpAddress(ipAddress);
            assessment.setRiskScore(riskScore);
            assessment.setRiskLevel(riskLevel);
            assessment.setRiskFactors(factors);
            assessment.setDeviceFingerprint(deviceFingerprint);

            // Add geolocation data
            if (ipReputation.containsKey("country")) {
                assessment.setGeolocationCountry((String) ipReputation.get("country"));
                assessment.setGeolocationCity((String) ipReputation.get("city"));
                assessment.setGeolocationLat((Double) ipReputation.get("lat"));
                assessment.setGeolocationLon((Double) ipReputation.get("lon"));
            }

            assessment.setIsVpn(isVpn);
            assessment.setIsProxy(isProxy);
            assessment.setIsTor(isTor);
            assessment.setIpReputationScore((Integer) ipReputation.getOrDefault("abuseScore", 0));

            // Determine action
            String action;
            if (riskScore >= criticalRiskThreshold) {
                action = "ACCOUNT_LOCKED";
                lockAccount(user.getId(), "High-risk login detected (score: " + riskScore + ")", accountLockMinutes);

                // Send email alert
                sendThreatEmail(user, assessment, "CRITICAL");
            } else if (riskScore >= highRiskThreshold) {
                action = "FLAGGED";
                sendThreatEmail(user, assessment, "HIGH");
            } else {
                action = "ALLOWED";
            }

            assessment.setActionTaken(action);
            threatAssessmentRepository.save(assessment);

            // Log audit event
            if (riskScore >= highRiskThreshold) {
                logAuditEvent(AuditEventType.HIGH_RISK_LOGIN, user.getId(), user.getUsername(),
                        user.getRole().name(), "High-risk login detected with score: " + riskScore);
            }

            log.info("Threat assessment completed for user: {}, Risk Score: {}, Action: {}",
                    user.getUsername(), riskScore, action);

        } catch (Exception e) {
            log.error("Failed to assess threat for user: {}", user.getUsername(), e);
        }
    }

    @Override
    public int calculateThreatScore(Map<String, Object> factors) {
        int score = 0;

        // IP reputation (0-40 points)
        Map<String, Object> ipReputation = (Map<String, Object>) factors.get("ipReputation");
        if (ipReputation != null) {
            Integer abuseScore = (Integer) ipReputation.getOrDefault("abuseScore", 0);
            score += Math.min(abuseScore / 3, 40); // Scale 0-100 abuse score to 0-40 points
        }

        // VPN/Proxy/Tor (15 points each, max 30)
        if ((Boolean) factors.getOrDefault("isVpn", false)) {
            score += 15;
        }
        if ((Boolean) factors.getOrDefault("isProxy", false)) {
            score += 15;
        }
        if ((Boolean) factors.getOrDefault("isTor", false)) {
            score += 30; // Tor is higher risk
        }

        // Location anomaly (20 points)
        if ((Boolean) factors.getOrDefault("locationAnomaly", false)) {
            score += 20;
        }

        // Recent failed logins (10 points)
        Long recentFailedLogins = (Long) factors.getOrDefault("recentFailedLogins", 0L);
        if (recentFailedLogins > 3) {
            score += 10;
        }

        // New device (10 points)
        if ((Boolean) factors.getOrDefault("newDevice", false)) {
            score += 10;
        }

        // Unusual time (5 points)
        if ((Boolean) factors.getOrDefault("unusualTime", false)) {
            score += 5;
        }

        return Math.min(score, 100); // Cap at 100
    }

    @Override
    @Transactional
    public void lockAccount(Long userId, String reason, int minutes) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setIsAccountLocked(true);
        user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(minutes));
        user.setLockReason(reason);
        userRepository.save(user);

        // Log audit event
        logAuditEvent(AuditEventType.ACCOUNT_LOCKED, userId, user.getUsername(),
                user.getRole().name(), "Account locked: " + reason);

        log.warn("Account locked for user: {}, Reason: {}", user.getUsername(), reason);
    }

    @Override
    @Transactional
    public void unlockAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setIsAccountLocked(false);
        user.setAccountLockedUntil(null);
        user.setLockReason(null);
        userRepository.save(user);

        // Log audit event
        logAuditEvent(AuditEventType.ACCOUNT_UNLOCKED, userId, user.getUsername(),
                user.getRole().name(), "Account unlocked by administrator");

        log.info("Account unlocked for user: {}", user.getUsername());
    }

    @Override
    @Scheduled(cron = "0 * * * * *") // Every minute
    @Transactional
    public void checkAndUnlockExpiredLocks() {
        List<User> lockedUsers = userRepository.findAll().stream()
                .filter(u -> u.getIsAccountLocked() != null && u.getIsAccountLocked())
                .filter(u -> u.getAccountLockedUntil() != null &&
                        u.getAccountLockedUntil().isBefore(LocalDateTime.now()))
                .toList();

        for (User user : lockedUsers) {
            user.setIsAccountLocked(false);
            user.setAccountLockedUntil(null);
            userRepository.save(user);

            logAuditEvent(AuditEventType.ACCOUNT_UNLOCKED, user.getId(), user.getUsername(),
                    user.getRole().name(), "Account automatically unlocked after timeout");

            log.info("Account automatically unlocked for user: {}", user.getUsername());
        }
    }

    @Override
    public Page<ThreatAssessment> getThreatAssessments(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return threatAssessmentRepository.findByUserOrderByAssessedAtDesc(user, pageable);
    }

    @Override
    public Page<ThreatAssessment> getHighRiskAssessments(int minScore, Pageable pageable) {
        return threatAssessmentRepository.findHighRiskAssessments(minScore, pageable);
    }

    @Override
    public List<ThreatAssessment> getThreatAssessmentsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return threatAssessmentRepository.findByDateRange(startDate, endDate);
    }

    @Override
    public Map<String, Object> getIPReputation(String ipAddress) {
        // Check cache first
        Optional<IPReputationCache> cachedOpt = ipCacheRepository
                .findByIpAddressAndExpiresAtAfter(ipAddress, LocalDateTime.now());

        if (cachedOpt.isPresent()) {
            IPReputationCache cached = cachedOpt.get();
            Map<String, Object> result = new HashMap<>(cached.getRawData());
            result.put("cached", true);
            log.debug("IP reputation cache hit for: {}", ipAddress);
            return result;
        }

        // Fetch from external APIs
        Map<String, Object> reputation = fetchIPReputationFromAPIs(ipAddress);

        // Cache the result
        cacheIPReputation(ipAddress, reputation);

        reputation.put("cached", false);
        return reputation;
    }

    @Override
    @Transactional
    public void clearIPReputationCache() {
        ipCacheRepository.deleteAll();
        log.info("IP reputation cache cleared");
    }

    // Private helper methods

    private Map<String, Object> fetchIPReputationFromAPIs(String ipAddress) {
        Map<String, Object> reputation = new HashMap<>();

        // Skip for local/private IPs
        if (isPrivateIP(ipAddress)) {
            reputation.put("isPrivate", true);
            reputation.put("abuseScore", 0);
            reputation.put("isVpn", false);
            reputation.put("isProxy", false);
            reputation.put("isTor", false);
            return reputation;
        }

        // 1. Get geolocation from IP-API (free, no auth)
        try {
            Map<String, Object> geoData = fetchGeolocation(ipAddress);
            reputation.putAll(geoData);
        } catch (Exception e) {
            log.warn("Failed to fetch geolocation for IP: {}", ipAddress, e);
        }

        // 2. Get abuse score from AbuseIPDB (requires API key)
        if (abuseIpDbApiKey != null && !abuseIpDbApiKey.isEmpty()) {
            try {
                Map<String, Object> abuseData = fetchAbuseIPDB(ipAddress);
                reputation.putAll(abuseData);
            } catch (Exception e) {
                log.warn("Failed to fetch AbuseIPDB data for IP: {}", ipAddress, e);
                reputation.put("abuseScore", 0);
            }
        } else {
            reputation.put("abuseScore", 0);
        }

        return reputation;
    }

    private Map<String, Object> fetchGeolocation(String ipAddress) throws Exception {
        String url = ipApiUrl + "/" + ipAddress + "?fields=status,country,countryCode,city,lat,lon,proxy,hosting";

        Request request = new Request.Builder().url(url).build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("Geolocation API failed: " + response.code());
            }

            JsonNode json = objectMapper.readTree(response.body().string());

            Map<String, Object> result = new HashMap<>();
            result.put("country", json.has("countryCode") ? json.get("countryCode").asText() : null);
            result.put("city", json.has("city") ? json.get("city").asText() : null);
            result.put("lat", json.has("lat") ? json.get("lat").asDouble() : null);
            result.put("lon", json.has("lon") ? json.get("lon").asDouble() : null);
            result.put("isProxy", json.has("proxy") && json.get("proxy").asBoolean());
            result.put("isVpn", json.has("hosting") && json.get("hosting").asBoolean());
            result.put("isTor", false); // IP-API doesn't detect Tor

            return result;
        }
    }

    private Map<String, Object> fetchAbuseIPDB(String ipAddress) throws Exception {
        String url = abuseIpDbUrl + "?ipAddress=" + ipAddress;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Key", abuseIpDbApiKey)
                .addHeader("Accept", "application/json")
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new RuntimeException("AbuseIPDB API failed: " + response.code());
            }

            JsonNode json = objectMapper.readTree(response.body().string());
            JsonNode data = json.get("data");

            Map<String, Object> result = new HashMap<>();
            result.put("abuseScore", data.has("abuseConfidenceScore") ?
                    data.get("abuseConfidenceScore").asInt() : 0);
            result.put("isTor", data.has("isTor") && data.get("isTor").asBoolean());

            return result;
        }
    }

    @Transactional
    private void cacheIPReputation(String ipAddress, Map<String, Object> reputation) {
        IPReputationCache cache = new IPReputationCache();
        cache.setIpAddress(ipAddress);
        cache.setReputationScore((Integer) reputation.getOrDefault("abuseScore", 0));
        cache.setIsMalicious(cache.getReputationScore() >= 80);
        cache.setIsVpn((Boolean) reputation.getOrDefault("isVpn", false));
        cache.setIsProxy((Boolean) reputation.getOrDefault("isProxy", false));
        cache.setIsTor((Boolean) reputation.getOrDefault("isTor", false));
        cache.setCountryCode((String) reputation.get("country"));
        cache.setCity((String) reputation.get("city"));
        cache.setLatitude((Double) reputation.get("lat"));
        cache.setLongitude((Double) reputation.get("lon"));
        cache.setRawData(reputation);
        cache.setExpiresAt(LocalDateTime.now().plusHours(cacheTtlHours));

        ipCacheRepository.save(cache);
    }

    private boolean checkLocationAnomaly(User user, String ipAddress, Map<String, Object> ipReputation) {
        // Get user's recent sessions
        List<UserSession> recentSessions = userSessionRepository.findRecentSessionsByUserId(
                user.getId(), LocalDateTime.now().minusDays(30));

        if (recentSessions.isEmpty()) {
            return false; // First login, no baseline
        }

        String currentCountry = (String) ipReputation.get("country");
        if (currentCountry == null) {
            return false;
        }

        // Check if this country is new
        boolean countryMatch = recentSessions.stream()
                .anyMatch(s -> currentCountry.equals(s.getCountry()));

        return !countryMatch; // Anomaly if country is new
    }

    private boolean checkNewDevice(User user, String deviceFingerprint) {
        List<UserSession> sessions = userSessionRepository.findByUserId(user.getId());
        return sessions.stream()
                .noneMatch(s -> deviceFingerprint.equals(s.getDeviceFingerprint()));
    }

    private boolean checkUnusualLoginTime() {
        int hour = LocalDateTime.now().getHour();
        // Consider 2 AM - 6 AM as unusual
        return hour >= 2 && hour < 6;
    }

    private String determineRiskLevel(int score) {
        if (score >= criticalRiskThreshold) {
            return "CRITICAL";
        } else if (score >= highRiskThreshold) {
            return "HIGH";
        } else if (score >= 40) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }

    private boolean isPrivateIP(String ipAddress) {
        return ipAddress.startsWith("10.") ||
                ipAddress.startsWith("192.168.") ||
                ipAddress.startsWith("172.") ||
                ipAddress.equals("127.0.0.1") ||
                ipAddress.equals("localhost");
    }

    private void sendThreatEmail(User user, ThreatAssessment assessment, String severity) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("riskLevel", assessment.getRiskLevel());
        variables.put("riskScore", assessment.getRiskScore());
        variables.put("ipAddress", assessment.getIpAddress());
        variables.put("location", assessment.getGeolocationCity() + ", " + assessment.getGeolocationCountry());
        variables.put("timestamp", assessment.getAssessedAt().toString());

        if ("CRITICAL".equals(severity)) {
            emailService.queueEmail(user, "email/account-locked", variables, 1);
        } else {
            emailService.queueEmail(user, "email/security-alert", variables, 2);
        }
    }

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

    @Scheduled(cron = "0 0 * * * *") // Every hour
    @Transactional
    public void cleanupExpiredCache() {
        int deleted = ipCacheRepository.deleteExpiredCache(LocalDateTime.now());
        if (deleted > 0) {
            log.info("Cleaned up {} expired IP reputation cache entries", deleted);
        }
    }
}
