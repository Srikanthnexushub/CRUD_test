package org.example.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Security Headers Configuration
 * Implements comprehensive security headers for defense-in-depth
 */
@Configuration
public class SecurityHeadersConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(securityHeadersInterceptor());
    }

    @Bean
    public HandlerInterceptor securityHeadersInterceptor() {
        return (request, response, handler) -> {
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            // Content Security Policy (CSP)
            // Strict policy to prevent XSS attacks
            httpResponse.setHeader("Content-Security-Policy",
                "default-src 'self'; " +
                "script-src 'self' 'unsafe-inline' 'unsafe-eval' https://cdn.jsdelivr.net; " +
                "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net https://fonts.googleapis.com; " +
                "font-src 'self' https://fonts.gstatic.com; " +
                "img-src 'self' data: https:; " +
                "connect-src 'self' https://api.github.com; " +
                "frame-ancestors 'none'; " +
                "base-uri 'self'; " +
                "form-action 'self'");

            // X-Frame-Options: Prevent clickjacking attacks
            httpResponse.setHeader("X-Frame-Options", "DENY");

            // X-Content-Type-Options: Prevent MIME type sniffing
            httpResponse.setHeader("X-Content-Type-Options", "nosniff");

            // X-XSS-Protection: Enable browser XSS protection (legacy browsers)
            httpResponse.setHeader("X-XSS-Protection", "1; mode=block");

            // Strict-Transport-Security (HSTS): Force HTTPS
            // max-age=31536000 (1 year), includeSubDomains, preload
            httpResponse.setHeader("Strict-Transport-Security",
                "max-age=31536000; includeSubDomains; preload");

            // Referrer-Policy: Control referrer information
            httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

            // Permissions-Policy: Disable unnecessary browser features
            httpResponse.setHeader("Permissions-Policy",
                "geolocation=(), microphone=(), camera=(), payment=(), usb=(), " +
                "magnetometer=(), gyroscope=(), speaker=(), vibrate=()");

            // X-Permitted-Cross-Domain-Policies: Restrict Adobe Flash/PDF
            httpResponse.setHeader("X-Permitted-Cross-Domain-Policies", "none");

            // Cache-Control: Prevent sensitive data caching
            if (request.getRequestURI().contains("/api/")) {
                httpResponse.setHeader("Cache-Control",
                    "no-store, no-cache, must-revalidate, proxy-revalidate");
                httpResponse.setHeader("Pragma", "no-cache");
                httpResponse.setHeader("Expires", "0");
            }

            // Cross-Origin-Embedder-Policy (COEP)
            httpResponse.setHeader("Cross-Origin-Embedder-Policy", "require-corp");

            // Cross-Origin-Opener-Policy (COOP)
            httpResponse.setHeader("Cross-Origin-Opener-Policy", "same-origin");

            // Cross-Origin-Resource-Policy (CORP)
            httpResponse.setHeader("Cross-Origin-Resource-Policy", "same-origin");

            return true;
        };
    }
}
