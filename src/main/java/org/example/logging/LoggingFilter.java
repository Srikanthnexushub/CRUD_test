package org.example.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter for structured HTTP request/response logging with correlation IDs.
 */
@Component
@Slf4j
public class LoggingFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String CORRELATION_ID_KEY = "correlationId";
    private static final String REQUEST_ID_KEY = "requestId";
    private static final String USER_ID_KEY = "userId";
    private static final String IP_ADDRESS_KEY = "ipAddress";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Generate or extract correlation ID
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isEmpty()) {
            correlationId = UUID.randomUUID().toString();
        }

        // Generate request ID
        String requestId = UUID.randomUUID().toString();

        // Extract IP address
        String ipAddress = getClientIP(request);

        // Add to MDC (Mapped Diagnostic Context) for logging
        MDC.put(CORRELATION_ID_KEY, correlationId);
        MDC.put(REQUEST_ID_KEY, requestId);
        MDC.put(IP_ADDRESS_KEY, ipAddress);

        // Add correlation ID to response headers
        response.setHeader(CORRELATION_ID_HEADER, correlationId);

        // Wrap request and response for content caching
        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        long startTime = System.currentTimeMillis();

        try {
            // Log incoming request
            logRequest(requestWrapper, correlationId, requestId, ipAddress);

            // Continue filter chain
            filterChain.doFilter(requestWrapper, responseWrapper);

        } finally {
            long duration = System.currentTimeMillis() - startTime;

            // Log outgoing response
            logResponse(responseWrapper, correlationId, requestId, duration);

            // Copy cached response content to actual response
            responseWrapper.copyBodyToResponse();

            // Clear MDC
            MDC.clear();
        }
    }

    private void logRequest(HttpServletRequest request, String correlationId, String requestId, String ipAddress) {
        log.info("Incoming HTTP Request",
                StructuredArguments.kv("correlation_id", correlationId),
                StructuredArguments.kv("request_id", requestId),
                StructuredArguments.kv("http_method", request.getMethod()),
                StructuredArguments.kv("request_uri", request.getRequestURI()),
                StructuredArguments.kv("query_string", request.getQueryString()),
                StructuredArguments.kv("ip_address", ipAddress),
                StructuredArguments.kv("user_agent", request.getHeader("User-Agent")),
                StructuredArguments.kv("content_type", request.getContentType())
        );
    }

    private void logResponse(HttpServletResponse response, String correlationId, String requestId, long duration) {
        log.info("Outgoing HTTP Response",
                StructuredArguments.kv("correlation_id", correlationId),
                StructuredArguments.kv("request_id", requestId),
                StructuredArguments.kv("status_code", response.getStatus()),
                StructuredArguments.kv("duration_ms", duration),
                StructuredArguments.kv("content_type", response.getContentType())
        );
    }

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
        // Skip logging for actuator endpoints
        String path = request.getRequestURI();
        return path.startsWith("/actuator/");
    }
}
