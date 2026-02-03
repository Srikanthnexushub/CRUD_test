package org.example.filter;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Request Throttling Filter
 * Additional layer of protection against request flooding
 * Works alongside Bucket4j rate limiting for defense-in-depth
 */
@Component
@Order(3)
@Slf4j
public class RequestThrottlingFilter extends OncePerRequestFilter {

    // Cache to track requests per IP
    private final LoadingCache<String, AtomicInteger> requestCounts;

    // Maximum requests per window
    private static final int MAX_REQUESTS_PER_WINDOW = 1000;

    // Time window in seconds
    private static final int TIME_WINDOW_SECONDS = 60;

    public RequestThrottlingFilter() {
        this.requestCounts = CacheBuilder.newBuilder()
            .expireAfterWrite(TIME_WINDOW_SECONDS, TimeUnit.SECONDS)
            .build(new CacheLoader<String, AtomicInteger>() {
                @Override
                public AtomicInteger load(String key) {
                    return new AtomicInteger(0);
                }
            });
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String clientIp = getClientIp(request);

        try {
            AtomicInteger counter = requestCounts.get(clientIp);
            int currentCount = counter.incrementAndGet();

            if (currentCount > MAX_REQUESTS_PER_WINDOW) {
                log.warn("Request flooding detected from IP: {} (count: {})", clientIp, currentCount);
                response.setStatus(429); // Too Many Requests
                response.setHeader("Retry-After", String.valueOf(TIME_WINDOW_SECONDS));
                response.getWriter().write("{\"error\":\"Too many requests. Please slow down.\"}");
                return;
            }

            // Add request count to response headers for transparency
            response.setHeader("X-RateLimit-Limit", String.valueOf(MAX_REQUESTS_PER_WINDOW));
            response.setHeader("X-RateLimit-Remaining",
                String.valueOf(Math.max(0, MAX_REQUESTS_PER_WINDOW - currentCount)));

        } catch (ExecutionException e) {
            log.error("Error in request throttling", e);
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
