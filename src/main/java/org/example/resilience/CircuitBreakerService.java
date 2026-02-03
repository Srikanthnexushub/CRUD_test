package org.example.resilience;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service demonstrating circuit breaker, retry, and bulkhead patterns.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CircuitBreakerService {

    /**
     * Database operation with circuit breaker and retry.
     * If database is down, circuit breaker opens after 50% failure rate.
     */
    @CircuitBreaker(name = "database", fallbackMethod = "databaseFallback")
    @Retry(name = "database")
    @Bulkhead(name = "database")
    public String performDatabaseOperation(String operation) {
        log.debug("Executing database operation: {}", operation);

        // Simulate database operation
        // In real implementation, this would call actual database
        if (Math.random() > 0.9) { // 10% failure simulation
            throw new RuntimeException("Database connection failed");
        }

        return "Database operation successful: " + operation;
    }

    /**
     * Fallback method for database operations.
     * Returns cached or default response when database is unavailable.
     */
    private String databaseFallback(String operation, Exception ex) {
        log.warn("Database fallback triggered for operation: {} - Error: {}", operation, ex.getMessage());
        return "Using cached data for: " + operation;
    }

    /**
     * External API call with circuit breaker, retry, and time limit.
     */
    @CircuitBreaker(name = "external-api", fallbackMethod = "externalApiFallback")
    @Retry(name = "external-api")
    @Bulkhead(name = "external-api")
    public String callExternalApi(String endpoint) {
        log.debug("Calling external API: {}", endpoint);

        // Simulate external API call
        // In real implementation, this would use RestTemplate or WebClient
        if (Math.random() > 0.85) { // 15% failure simulation
            throw new RuntimeException("External API timeout");
        }

        return "API response from: " + endpoint;
    }

    /**
     * Fallback method for external API calls.
     * Returns default response when external service is unavailable.
     */
    private String externalApiFallback(String endpoint, Exception ex) {
        log.warn("External API fallback triggered for: {} - Error: {}", endpoint, ex.getMessage());
        return "Service temporarily unavailable. Using default response.";
    }

    /**
     * Email sending with circuit breaker.
     */
    @CircuitBreaker(name = "email", fallbackMethod = "emailFallback")
    @Retry(name = "external-api") // Reuse retry config
    public boolean sendEmail(String to, String subject, String body) {
        log.debug("Sending email to: {}", to);

        // Simulate email sending
        // In real implementation, this would use JavaMailSender
        if (Math.random() > 0.95) { // 5% failure simulation
            throw new RuntimeException("SMTP server unavailable");
        }

        return true;
    }

    /**
     * Fallback method for email sending.
     * Queues email for later sending or sends notification to admin.
     */
    private boolean emailFallback(String to, String subject, String body, Exception ex) {
        log.warn("Email fallback triggered for: {} - Error: {}", to, ex.getMessage());
        // In production: Queue email for later delivery
        return false;
    }
}
