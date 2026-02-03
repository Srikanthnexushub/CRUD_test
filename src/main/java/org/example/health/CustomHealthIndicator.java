package org.example.health;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Custom health indicator for application-specific health checks.
 */
@Component("application")
@RequiredArgsConstructor
public class CustomHealthIndicator implements HealthIndicator {

    private final JdbcTemplate jdbcTemplate;
    private final Instant startupTime = Instant.now();

    @Override
    public Health health() {
        try {
            // Check database connectivity
            boolean dbHealthy = checkDatabaseHealth();

            // Check application uptime
            Duration uptime = Duration.between(startupTime, Instant.now());

            // Check memory usage
            Runtime runtime = Runtime.getRuntime();
            long usedMemory = runtime.totalMemory() - runtime.freeMemory();
            long maxMemory = runtime.maxMemory();
            double memoryUsagePercent = (double) usedMemory / maxMemory * 100;

            Health.Builder healthBuilder;

            if (!dbHealthy) {
                healthBuilder = Health.down().withDetail("database", "Database connectivity failed");
            } else if (memoryUsagePercent > 90) {
                healthBuilder = Health.status(Status.OUT_OF_SERVICE)
                        .withDetail("memory", "Memory usage critical: " + String.format("%.2f%%", memoryUsagePercent));
            } else if (memoryUsagePercent > 80) {
                healthBuilder = Health.status("WARNING")
                        .withDetail("memory", "Memory usage high: " + String.format("%.2f%%", memoryUsagePercent));
            } else {
                healthBuilder = Health.up();
            }

            return healthBuilder
                    .withDetail("uptime_seconds", uptime.getSeconds())
                    .withDetail("uptime_formatted", formatDuration(uptime))
                    .withDetail("memory_used_mb", usedMemory / 1024 / 1024)
                    .withDetail("memory_max_mb", maxMemory / 1024 / 1024)
                    .withDetail("memory_usage_percent", String.format("%.2f%%", memoryUsagePercent))
                    .withDetail("database_connected", dbHealthy)
                    .withDetail("startup_time", startupTime.toString())
                    .build();

        } catch (Exception e) {
            return Health.down()
                    .withDetail("error", e.getMessage())
                    .withException(e)
                    .build();
        }
    }

    private boolean checkDatabaseHealth() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String formatDuration(Duration duration) {
        long days = duration.toDays();
        long hours = duration.toHoursPart();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();

        if (days > 0) {
            return String.format("%dd %dh %dm %ds", days, hours, minutes, seconds);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds);
        } else {
            return String.format("%ds", seconds);
        }
    }
}
