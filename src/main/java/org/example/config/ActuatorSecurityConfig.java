package org.example.config;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security configuration for Actuator endpoints.
 * Separate security chain with higher priority for actuator endpoints.
 */
@Configuration
@EnableWebSecurity
@Order(1) // Higher priority than main security config
public class ActuatorSecurityConfig {

    @Bean
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher(EndpointRequest.toAnyEndpoint())
                .authorizeHttpRequests(authorize -> authorize
                        // Public health endpoint (no authentication required)
                        .requestMatchers(EndpointRequest.to("health", "info")).permitAll()

                        // Protected endpoints (require authentication)
                        .anyRequest().hasRole("ADMIN")
                )
                .httpBasic(basic -> {});  // Use HTTP Basic for actuator endpoints

        return http.build();
    }
}
