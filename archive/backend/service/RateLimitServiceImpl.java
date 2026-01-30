package org.example.service;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.RateLimitLog;
import org.example.entity.RateLimitWhitelist;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.repository.RateLimitLogRepository;
import org.example.repository.RateLimitWhitelistRepository;
import org.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitServiceImpl implements RateLimitService {

    private final RateLimitLogRepository rateLimitLogRepository;
    private final RateLimitWhitelistRepository whitelistRepository;
    private final UserRepository userRepository;

    @Value("${ratelimit.user.standard}")
    private int standardUserLimit;

    @Value("${ratelimit.user.admin}")
    private int adminUserLimit;

    @Value("${ratelimit.ip.login}")
    private int loginIpLimit;

    @Value("${ratelimit.ip.register}")
    private int registerIpLimit;

    @Value("${ratelimit.ip.global}")
    private int globalIpLimit;

    @Value("${ratelimit.window.minutes}")
    private int windowMinutes;

    @Value("${ratelimit.enabled}")
    private boolean rateLimitEnabled;

    // In-memory bucket storage
    private final Map<String, Bucket> userBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> endpointBuckets = new ConcurrentHashMap<>();

    @Override
    public Map<String, Object> checkRateLimit(HttpServletRequest request, Authentication authentication) {
        if (!rateLimitEnabled) {
            return createAllowedResponse(Integer.MAX_VALUE, Integer.MAX_VALUE, 0L);
        }

        String ipAddress = getClientIpAddress(request);
        String endpoint = request.getRequestURI();
        String method = request.getMethod();

        // Check whitelist first
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            if (isWhitelisted(ipAddress, username)) {
                return createAllowedResponse(Integer.MAX_VALUE, Integer.MAX_VALUE, 0L);
            }
        } else {
            if (isWhitelisted(ipAddress, null)) {
                return createAllowedResponse(Integer.MAX_VALUE, Integer.MAX_VALUE, 0L);
            }
        }

        // Determine rate limit based on endpoint and user role
        Bucket bucket;
        String limitType;
        int limitThreshold;

        if (authentication != null && authentication.isAuthenticated()) {
            // User-based rate limiting
            String username = authentication.getName();
            User user = userRepository.findByUsername(username).orElse(null);

            if (user != null && user.getRole() == Role.ROLE_ADMIN) {
                limitThreshold = adminUserLimit;
            } else {
                limitThreshold = standardUserLimit;
            }

            bucket = userBuckets.computeIfAbsent(username, k -> createBucket(limitThreshold));
            limitType = "USER";
        } else {
            // IP-based rate limiting for unauthenticated requests
            if (endpoint.contains("/login")) {
                limitThreshold = loginIpLimit;
            } else if (endpoint.contains("/register")) {
                limitThreshold = registerIpLimit;
            } else {
                limitThreshold = globalIpLimit;
            }

            String key = ipAddress + ":" + endpoint;
            bucket = ipBuckets.computeIfAbsent(key, k -> createBucket(limitThreshold));
            limitType = "IP";
        }

        // Try to consume token
        if (bucket.tryConsume(1)) {
            long remaining = bucket.getAvailableTokens();
            long resetTime = System.currentTimeMillis() + (windowMinutes * 60 * 1000);

            // Log successful request (async)
            logRateLimit(request, authentication, limitType, limitThreshold, (int) remaining, false);

            return createAllowedResponse(limitThreshold, (int) remaining, resetTime);
        } else {
            // Rate limit exceeded
            long resetTime = System.currentTimeMillis() + (windowMinutes * 60 * 1000);

            // Log violation (async)
            logRateLimit(request, authentication, limitType, limitThreshold, 0, true);

            return createBlockedResponse(limitThreshold, 0, resetTime);
        }
    }

    @Override
    public boolean isWhitelisted(String ipAddress, String username) {
        LocalDateTime now = LocalDateTime.now();

        // Check IP whitelist
        if (ipAddress != null) {
            boolean ipWhitelisted = whitelistRepository.existsByIpAddressAndIsActiveTrue(ipAddress);
            if (ipWhitelisted) {
                return true;
            }
        }

        // Check user whitelist
        if (username != null) {
            User user = userRepository.findByUsername(username).orElse(null);
            if (user != null) {
                boolean userWhitelisted = whitelistRepository.existsByUserAndIsActiveTrue(user);
                if (userWhitelisted) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    @Transactional
    public void addToWhitelist(String ipAddress, String reason, String createdBy, Integer expiresInDays) {
        RateLimitWhitelist whitelist = new RateLimitWhitelist();
        whitelist.setIpAddress(ipAddress);
        whitelist.setReason(reason);
        whitelist.setCreatedBy(createdBy);
        whitelist.setIsActive(true);

        if (expiresInDays != null) {
            whitelist.setExpiresAt(LocalDateTime.now().plusDays(expiresInDays));
        }

        whitelistRepository.save(whitelist);
        log.info("IP {} added to rate limit whitelist by {}", ipAddress, createdBy);
    }

    @Override
    @Transactional
    public void addUserToWhitelist(Long userId, String reason, String createdBy, Integer expiresInDays) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RateLimitWhitelist whitelist = new RateLimitWhitelist();
        whitelist.setUser(user);
        whitelist.setReason(reason);
        whitelist.setCreatedBy(createdBy);
        whitelist.setIsActive(true);

        if (expiresInDays != null) {
            whitelist.setExpiresAt(LocalDateTime.now().plusDays(expiresInDays));
        }

        whitelistRepository.save(whitelist);
        log.info("User {} added to rate limit whitelist by {}", user.getUsername(), createdBy);
    }

    @Override
    @Transactional
    public void removeFromWhitelist(Long whitelistId) {
        RateLimitWhitelist whitelist = whitelistRepository.findById(whitelistId)
                .orElseThrow(() -> new RuntimeException("Whitelist entry not found"));

        whitelist.setIsActive(false);
        whitelistRepository.save(whitelist);

        log.info("Whitelist entry {} removed", whitelistId);
    }

    @Override
    public List<Map<String, Object>> getWhitelist() {
        List<RateLimitWhitelist> whitelist = whitelistRepository.findAllActive(LocalDateTime.now());

        return whitelist.stream().map(entry -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", entry.getId());
            map.put("ipAddress", entry.getIpAddress());
            map.put("userId", entry.getUser() != null ? entry.getUser().getId() : null);
            map.put("username", entry.getUser() != null ? entry.getUser().getUsername() : null);
            map.put("reason", entry.getReason());
            map.put("createdBy", entry.getCreatedBy());
            map.put("expiresAt", entry.getExpiresAt());
            map.put("createdAt", entry.getCreatedAt());
            return map;
        }).toList();
    }

    @Override
    public Map<String, Object> getStatistics() {
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
        LocalDateTime last7Days = LocalDateTime.now().minusDays(7);

        long totalViolations24h = rateLimitLogRepository.countBlockedInDateRange(last24Hours, LocalDateTime.now());
        long totalViolations7d = rateLimitLogRepository.countBlockedInDateRange(last7Days, LocalDateTime.now());

        List<Object[]> topBlockedEndpoints = rateLimitLogRepository.findTopBlockedEndpoints(PageRequest.of(0, 10));

        Map<String, Object> stats = new HashMap<>();
        stats.put("violations24Hours", totalViolations24h);
        stats.put("violations7Days", totalViolations7d);
        stats.put("topBlockedEndpoints", topBlockedEndpoints);
        stats.put("activeWhitelistEntries", whitelistRepository.findAllActive(LocalDateTime.now()).size());
        stats.put("config", Map.of(
                "standardUserLimit", standardUserLimit,
                "adminUserLimit", adminUserLimit,
                "loginIpLimit", loginIpLimit,
                "registerIpLimit", registerIpLimit,
                "globalIpLimit", globalIpLimit,
                "windowMinutes", windowMinutes
        ));

        return stats;
    }

    @Override
    public List<Map<String, Object>> getRecentViolations(int limit) {
        List<RateLimitLog> logs = rateLimitLogRepository.findByWasBlockedTrueOrderByCreatedAtDesc(
                PageRequest.of(0, limit)).getContent();

        return logs.stream().map(log -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", log.getId());
            map.put("ipAddress", log.getIpAddress());
            map.put("endpoint", log.getEndpoint());
            map.put("httpMethod", log.getHttpMethod());
            map.put("limitType", log.getLimitType());
            map.put("limitThreshold", log.getLimitThreshold());
            map.put("createdAt", log.getCreatedAt());
            map.put("userId", log.getUser() != null ? log.getUser().getId() : null);
            map.put("username", log.getUser() != null ? log.getUser().getUsername() : null);
            return map;
        }).toList();
    }

    // Private helper methods

    private Bucket createBucket(int capacity) {
        Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(capacity, Duration.ofMinutes(windowMinutes)));
        return Bucket.builder().addLimit(limit).build();
    }

    @Async
    @Transactional
    public void logRateLimit(HttpServletRequest request, Authentication authentication,
                             String limitType, int limitThreshold, int remaining, boolean wasBlocked) {
        try {
            RateLimitLog log = new RateLimitLog();
            log.setIpAddress(getClientIpAddress(request));
            log.setEndpoint(request.getRequestURI());
            log.setHttpMethod(request.getMethod());
            log.setLimitType(limitType);
            log.setCurrentCount(limitThreshold - remaining);
            log.setLimitThreshold(limitThreshold);
            log.setWasBlocked(wasBlocked);
            log.setResetTime(LocalDateTime.now().plusMinutes(windowMinutes));

            if (authentication != null && authentication.isAuthenticated()) {
                String username = authentication.getName();
                User user = userRepository.findByUsername(username).orElse(null);
                log.setUser(user);
            }

            rateLimitLogRepository.save(log);
        } catch (Exception e) {
            // Don't fail the request if logging fails
            log.error("Failed to log rate limit event", e);
        }
    }

    private Map<String, Object> createAllowedResponse(int limit, int remaining, long resetTime) {
        Map<String, Object> response = new HashMap<>();
        response.put("allowed", true);
        response.put("limit", limit);
        response.put("remaining", remaining);
        response.put("resetTime", resetTime);
        return response;
    }

    private Map<String, Object> createBlockedResponse(int limit, int remaining, long resetTime) {
        Map<String, Object> response = new HashMap<>();
        response.put("allowed", false);
        response.put("limit", limit);
        response.put("remaining", remaining);
        response.put("resetTime", resetTime);
        return response;
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                return ip.split(",")[0].trim();
            }
        }

        return request.getRemoteAddr();
    }
}
