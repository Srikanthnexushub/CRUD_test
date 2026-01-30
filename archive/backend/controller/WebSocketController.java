package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.repository.AuditLogRepository;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * WebSocket controller for handling real-time dashboard requests
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketController {

    private final AuditLogRepository auditLogRepository;

    /**
     * Handle requests for real-time statistics
     * Clients send message to /app/stats, receive response on /topic/stats
     */
    @MessageMapping("/stats")
    @SendTo("/topic/stats")
    public Map<String, Object> getRealtimeStats() {
        try {
            Map<String, Object> stats = new HashMap<>();

            // Total events today
            LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
            LocalDateTime now = LocalDateTime.now();
            long todayEvents = auditLogRepository.findByDateRange(startOfDay, now, null)
                .getTotalElements();

            // Failed logins in last 24 hours
            LocalDateTime last24Hours = now.minusHours(24);
            long recentFailedLogins = auditLogRepository.countFailedLoginAttempts("", last24Hours);

            stats.put("todayEvents", todayEvents);
            stats.put("failedLogins", recentFailedLogins);
            stats.put("timestamp", now);

            return stats;
        } catch (Exception e) {
            log.error("Failed to get realtime stats: {}", e.getMessage(), e);
            return Map.of("error", "Failed to retrieve statistics");
        }
    }
}
