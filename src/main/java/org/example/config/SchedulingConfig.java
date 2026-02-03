package org.example.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration for scheduled tasks.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // Enables @Scheduled annotations
}
