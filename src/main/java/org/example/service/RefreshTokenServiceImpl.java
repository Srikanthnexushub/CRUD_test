package org.example.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.LoginResponse;
import org.example.entity.RefreshToken;
import org.example.entity.User;
import org.example.repository.RefreshTokenRepository;
import org.example.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of refresh token service for JWT token rotation.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final UserService userService;

    @Value("${jwt.refresh-expiration:604800000}") // 7 days in milliseconds
    private long refreshTokenDurationMs;

    @Value("${app.security.refresh-token.max-active-tokens:5}")
    private int maxActiveTokensPerUser;

    @Override
    @Transactional
    public RefreshToken createRefreshToken(User user, HttpServletRequest request) {
        log.debug("Creating refresh token for user: {}", user.getUsername());

        // Check active token limit
        long activeCount = refreshTokenRepository.countActiveByUser(user);
        if (activeCount >= maxActiveTokensPerUser) {
            log.warn("User {} has {} active refresh tokens (max: {}). Revoking oldest tokens.",
                     user.getUsername(), activeCount, maxActiveTokensPerUser);

            // Revoke oldest tokens to maintain limit
            List<RefreshToken> activeTokens = refreshTokenRepository.findActiveByUser(user);
            int tokensToRevoke = (int) (activeCount - maxActiveTokensPerUser + 1);
            for (int i = activeTokens.size() - 1; i >= activeTokens.size() - tokensToRevoke && i >= 0; i--) {
                RefreshToken oldToken = activeTokens.get(i);
                oldToken.setRevokedAt(LocalDateTime.now());
                refreshTokenRepository.save(oldToken);
            }
        }

        // Create new refresh token
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiresAt(LocalDateTime.now().plusSeconds(refreshTokenDurationMs / 1000));

        if (request != null) {
            refreshToken.setIpAddress(getClientIP(request));
            refreshToken.setUserAgent(request.getHeader("User-Agent"));
        }

        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        log.info("Refresh token created for user: {} - Expires at: {}",
                 user.getUsername(), saved.getExpiresAt());

        return saved;
    }

    @Override
    public RefreshToken verifyRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));

        if (refreshToken.isRevoked()) {
            log.warn("Attempt to use revoked refresh token: {}", token);
            throw new IllegalArgumentException("Refresh token has been revoked");
        }

        if (refreshToken.isExpired()) {
            log.warn("Attempt to use expired refresh token: {}", token);
            throw new IllegalArgumentException("Refresh token has expired. Please login again.");
        }

        log.debug("Refresh token verified for user: {}", refreshToken.getUser().getUsername());
        return refreshToken;
    }

    @Override
    @Transactional
    public LoginResponse refreshAccessToken(String refreshTokenString, HttpServletRequest request) {
        log.info("Refreshing access token using refresh token");

        // Verify refresh token
        RefreshToken refreshToken = verifyRefreshToken(refreshTokenString);
        User user = refreshToken.getUser();

        // Generate new access token
        String newAccessToken = userService.generateAuthToken(user);

        // Token rotation: Create new refresh token
        RefreshToken newRefreshToken = createRefreshToken(user, request);

        // Revoke old refresh token and mark replacement
        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshToken.setReplacedBy(newRefreshToken.getToken());
        refreshTokenRepository.save(refreshToken);

        log.info("Access token refreshed for user: {} - Old token revoked, new token issued",
                 user.getUsername());

        // Create login response
        LoginResponse response = new LoginResponse();
        response.setToken(newAccessToken);
        response.setRefreshToken(newRefreshToken.getToken());
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setRole(user.getRole());

        return response;
    }

    @Override
    @Transactional
    public void revokeToken(String token) {
        log.info("Revoking refresh token");

        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));

        refreshToken.setRevokedAt(LocalDateTime.now());
        refreshTokenRepository.save(refreshToken);

        log.info("Refresh token revoked for user: {}", refreshToken.getUser().getUsername());
    }

    @Override
    @Transactional
    public int revokeAllUserTokens(User user) {
        log.info("Revoking all refresh tokens for user: {}", user.getUsername());

        int revokedCount = refreshTokenRepository.revokeAllByUser(user);

        log.info("Revoked {} refresh tokens for user: {}", revokedCount, user.getUsername());
        return revokedCount;
    }

    @Override
    public List<RefreshToken> getActiveTokens(User user) {
        return refreshTokenRepository.findActiveByUser(user);
    }

    @Override
    public long getActiveTokenCount(User user) {
        return refreshTokenRepository.countActiveByUser(user);
    }

    @Override
    @Transactional
    public int cleanupExpiredTokens() {
        log.info("Cleaning up expired and old revoked refresh tokens");

        LocalDateTime expirationDate = LocalDateTime.now();
        LocalDateTime revocationDate = LocalDateTime.now().minusDays(30); // Keep revoked tokens for 30 days

        int deleted = refreshTokenRepository.deleteExpiredOrOldRevoked(expirationDate, revocationDate);

        log.info("Deleted {} expired or old revoked refresh tokens", deleted);
        return deleted;
    }

    /**
     * Extract client IP from request.
     */
    private String getClientIP(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }
}
