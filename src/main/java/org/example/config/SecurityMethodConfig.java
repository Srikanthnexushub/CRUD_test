package org.example.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * Configuration for method-level security annotations.
 * Enables @PreAuthorize, @PostAuthorize, @Secured annotations.
 */
@Configuration
@EnableMethodSecurity
public class SecurityMethodConfig {
    // Enables method security with SpEL expressions
}
