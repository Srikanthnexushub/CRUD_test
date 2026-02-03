package org.example.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.service.ThreatIntelligenceService;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.HtmlUtils;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Input Sanitization Filter
 * Protects against common injection attacks (XSS, SQL Injection, etc.)
 */
@Component
@Order(2)
@Slf4j
@RequiredArgsConstructor
public class InputSanitizationFilter extends OncePerRequestFilter {

    private final ThreatIntelligenceService threatIntelligenceService;

    // Patterns for detecting malicious input
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
        "('.+(--|\\/\\*|\\*\\/|;|\\||\\|\\||&&))|" +
        "((SELECT|INSERT|UPDATE|DELETE|DROP|CREATE|ALTER|EXEC|EXECUTE|UNION|DECLARE)\\s+)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern XSS_PATTERN = Pattern.compile(
        "(<script[^>]*>.*?</script>)|" +
        "(<iframe[^>]*>.*?</iframe>)|" +
        "(javascript:)|" +
        "(on\\w+\\s*=)|" +
        "(<img[^>]+src[^>]*>)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
        "(\\.\\./)|" +
        "(\\.\\\\)|" +
        "(%2e%2e/)|" +
        "(%2e%2e\\\\)",
        Pattern.CASE_INSENSITIVE
    );

    private static final Pattern COMMAND_INJECTION_PATTERN = Pattern.compile(
        "(;\\s*(ls|cat|curl|wget|nc|bash|sh|cmd|powershell))|" +
        "(\\|\\s*(ls|cat|curl|wget|nc|bash|sh|cmd|powershell))|" +
        "(&&\\s*(ls|cat|curl|wget|nc|bash|sh|cmd|powershell))",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Skip for certain paths (e.g., static resources)
        String requestUri = request.getRequestURI();
        if (shouldSkipFilter(requestUri)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Check query parameters
        if (request.getQueryString() != null) {
            String queryString = request.getQueryString();

            String attackType = detectAttackType(queryString);
            if (attackType != null) {
                String ipAddress = getClientIp(request);
                log.warn("Malicious input detected in query string: {} from IP: {} - Type: {}",
                    requestUri, ipAddress, attackType);

                // Record in threat intelligence
                try {
                    threatIntelligenceService.recordSuspiciousActivity(
                        ipAddress,
                        attackType,
                        "Malicious input in query: " + requestUri
                    );
                } catch (Exception e) {
                    log.error("Failed to record suspicious activity", e);
                }

                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("{\"error\":\"Invalid input detected\"}");
                return;
            }
        }

        // Check request headers for injection attempts
        String userAgent = request.getHeader("User-Agent");
        String referer = request.getHeader("Referer");

        String headerAttack = null;
        if (userAgent != null) headerAttack = detectAttackType(userAgent);
        if (headerAttack == null && referer != null) headerAttack = detectAttackType(referer);

        if (headerAttack != null) {
            String ipAddress = getClientIp(request);
            log.warn("Malicious input detected in headers for request: {} from IP: {} - Type: {}",
                requestUri, ipAddress, headerAttack);

            // Record in threat intelligence
            try {
                threatIntelligenceService.recordSuspiciousActivity(
                    ipAddress,
                    headerAttack,
                    "Malicious input in headers: " + requestUri
                );
            } catch (Exception e) {
                log.error("Failed to record suspicious activity", e);
            }

            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\":\"Invalid input detected in headers\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Detect specific type of attack
     */
    private String detectAttackType(String input) {
        if (input == null || input.isEmpty()) {
            return null;
        }

        if (SQL_INJECTION_PATTERN.matcher(input).find()) {
            return "SQL_INJECTION";
        }
        if (XSS_PATTERN.matcher(input).find()) {
            return "XSS";
        }
        if (PATH_TRAVERSAL_PATTERN.matcher(input).find()) {
            return "PATH_TRAVERSAL";
        }
        if (COMMAND_INJECTION_PATTERN.matcher(input).find()) {
            return "COMMAND_INJECTION";
        }

        return null;
    }


    /**
     * Skip filter for certain paths
     */
    private boolean shouldSkipFilter(String requestUri) {
        return requestUri.startsWith("/actuator/") ||
               requestUri.startsWith("/swagger-ui") ||
               requestUri.startsWith("/api-docs") ||
               requestUri.startsWith("/v3/api-docs") ||
               requestUri.endsWith(".css") ||
               requestUri.endsWith(".js") ||
               requestUri.endsWith(".png") ||
               requestUri.endsWith(".jpg") ||
               requestUri.endsWith(".ico");
    }

    /**
     * Get client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
