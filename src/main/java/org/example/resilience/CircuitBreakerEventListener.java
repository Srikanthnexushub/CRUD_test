package org.example.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnErrorEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnSuccessEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.metrics.CustomMetrics;
import org.springframework.stereotype.Component;

/**
 * Event listener for circuit breaker state changes and metrics collection.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class CircuitBreakerEventListener {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final CustomMetrics customMetrics;

    @PostConstruct
    public void registerEventListeners() {
        circuitBreakerRegistry.getAllCircuitBreakers().forEach(circuitBreaker -> {
            String name = circuitBreaker.getName();

            // Listen for state transitions (CLOSED -> OPEN -> HALF_OPEN)
            circuitBreaker.getEventPublisher()
                    .onStateTransition(this::handleStateTransition);

            // Listen for successful calls
            circuitBreaker.getEventPublisher()
                    .onSuccess(event -> handleSuccess(name, event));

            // Listen for failed calls
            circuitBreaker.getEventPublisher()
                    .onError(event -> handleError(name, event));

            log.info("Registered event listeners for circuit breaker: {}", name);
        });
    }

    private void handleStateTransition(CircuitBreakerOnStateTransitionEvent event) {
        CircuitBreaker.State fromState = event.getStateTransition().getFromState();
        CircuitBreaker.State toState = event.getStateTransition().getToState();
        String circuitBreakerName = event.getCircuitBreakerName();

        log.warn("Circuit breaker '{}' state transition: {} -> {}",
                circuitBreakerName, fromState, toState);

        // Record metric
        customMetrics.recordBusinessEvent(
                "circuit_breaker_state_change",
                String.format("%s: %s->%s", circuitBreakerName, fromState, toState)
        );

        // Alert for OPEN state
        if (toState == CircuitBreaker.State.OPEN) {
            log.error("ALERT: Circuit breaker '{}' is now OPEN! Service degraded.", circuitBreakerName);
            // In production: Send alert to monitoring system (PagerDuty, Slack, etc.)
        }

        // Log recovery
        if (toState == CircuitBreaker.State.CLOSED && fromState != CircuitBreaker.State.CLOSED) {
            log.info("Circuit breaker '{}' recovered to CLOSED state", circuitBreakerName);
        }
    }

    private void handleSuccess(String name, CircuitBreakerOnSuccessEvent event) {
        log.debug("Circuit breaker '{}' - Successful call. Duration: {}ms",
                name, event.getElapsedDuration().toMillis());
    }

    private void handleError(String name, CircuitBreakerOnErrorEvent event) {
        log.warn("Circuit breaker '{}' - Failed call. Error: {} - Duration: {}ms",
                name,
                event.getThrowable().getClass().getSimpleName(),
                event.getElapsedDuration().toMillis());
    }
}
