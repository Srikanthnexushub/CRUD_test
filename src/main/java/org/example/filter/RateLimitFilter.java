package org.example.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.service.RateLimitService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter for rate limiting API requests.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String clientIP = getClientIP(request);
        String requestURI = request.getRequestURI();

        // Determine bucket type based on endpoint
        RateLimitService.BucketType bucketType = getBucketType(requestURI);

        // Check rate limit
        if (!rateLimitService.tryConsume(clientIP, bucketType)) {
            // Rate limit exceeded
            log.warn("Rate limit exceeded for IP: {} on endpoint: {}", clientIP, requestURI);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");

            long remainingTokens = rateLimitService.getRemainingTokens(clientIP, bucketType);

            String jsonResponse = String.format(
                    "{\"error\":\"Too many requests\",\"message\":\"Rate limit exceeded. Please try again later.\",\"remainingTokens\":%d}",
                    remainingTokens
            );

            response.getWriter().write(jsonResponse);
            return;
        }

        // Add rate limit headers
        long remainingTokens = rateLimitService.getRemainingTokens(clientIP, bucketType);
        response.setHeader("X-Rate-Limit-Remaining", String.valueOf(remainingTokens));

        filterChain.doFilter(request, response);
    }

    /**
     * Determine bucket type based on request URI.
     */
    private RateLimitService.BucketType getBucketType(String requestURI) {
        if (requestURI.startsWith("/api/auth/login") || requestURI.startsWith("/api/auth/register")) {
            return RateLimitService.BucketType.AUTH;
        } else if (requestURI.startsWith("/api/mfa/")) {
            return RateLimitService.BucketType.MFA;
        } else if (requestURI.startsWith("/api/")) {
            return RateLimitService.BucketType.API;
        } else {
            return RateLimitService.BucketType.GENERAL;
        }
    }

    /**
     * Extract client IP address from request.
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

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip rate limiting for health check and actuator endpoints
        String path = request.getRequestURI();
        return path.startsWith("/actuator/") ||
               path.equals("/health") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs/");
    }
}
