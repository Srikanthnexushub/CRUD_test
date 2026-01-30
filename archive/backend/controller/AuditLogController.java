package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.AuditEventType;
import org.example.entity.AuditLog;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.exception.UnauthorizedException;
import org.example.exception.UserNotFoundException;
import org.example.repository.UserRepository;
import org.example.service.AuditLogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/audit-logs")
@RequiredArgsConstructor
@Slf4j
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    /**
     * Get audit logs with filtering and pagination (Admin only)
     */
    @GetMapping
    public ResponseEntity<Page<AuditLog>> getAuditLogs(
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) AuditEventType eventType,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
        @RequestParam(required = false) String searchTerm,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "timestamp") String sortBy,
        @RequestParam(defaultValue = "DESC") String sortDirection,
        Authentication authentication
    ) {
        // Check if user is admin
        checkAdminAccess(authentication);

        log.info("Admin '{}' retrieving audit logs", authentication.getName());

        Pageable pageable = PageRequest.of(page, size);

        Page<AuditLog> auditLogs;

        // Use simple query if no filters, otherwise use search
        boolean hasFilters = userId != null || eventType != null || status != null ||
                            startDate != null || endDate != null ||
                            (searchTerm != null && !searchTerm.trim().isEmpty());

        if (hasFilters) {
            auditLogs = auditLogService.searchAuditLogs(
                userId, eventType, status, startDate, endDate, searchTerm, pageable
            );
        } else {
            // Use simple query without filters
            auditLogs = auditLogService.getAllAuditLogs(pageable);
        }

        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Get audit logs for a specific user (Admin only, or own logs)
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByUser(
        @PathVariable Long userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        Authentication authentication
    ) {
        User currentUser = userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new UserNotFoundException("Current user not found"));

        // Allow users to view their own logs, admins can view all
        if (!currentUser.getId().equals(userId) && currentUser.getRole() != Role.ROLE_ADMIN) {
            throw new UnauthorizedException("You can only view your own audit logs");
        }

        log.info("User '{}' retrieving audit logs for user ID: {}", authentication.getName(), userId);

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp"));

        Page<AuditLog> auditLogs = auditLogService.getAuditLogsByUserId(userId, pageable);

        return ResponseEntity.ok(auditLogs);
    }

    /**
     * Get SOC Dashboard stats (Admin only)
     */
    @GetMapping("/dashboard-stats")
    public ResponseEntity<Map<String, Object>> getDashboardStats(Authentication authentication) {
        checkAdminAccess(authentication);

        log.info("Admin '{}' retrieving SOC dashboard stats", authentication.getName());

        LocalDateTime startOfToday = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0);

        Pageable pageable = PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "timestamp"));

        // Get today's events
        Page<AuditLog> todayEvents = auditLogService.searchAuditLogs(
            null, null, null, startOfToday, null, null, pageable
        );

        // Get today's failed logins
        Page<AuditLog> failedLogins = auditLogService.searchAuditLogs(
            null, AuditEventType.LOGIN_FAILURE, null, startOfToday, null, null, pageable
        );

        // Get today's critical alerts (LOGIN_FAILURE, ACCESS_DENIED, SUSPICIOUS_ACTIVITY)
        Page<AuditLog> accessDenied = auditLogService.searchAuditLogs(
            null, AuditEventType.ACCESS_DENIED, null, startOfToday, null, null, pageable
        );
        Page<AuditLog> suspicious = auditLogService.searchAuditLogs(
            null, AuditEventType.SUSPICIOUS_ACTIVITY, null, startOfToday, null, null, pageable
        );

        long criticalAlerts = failedLogins.getTotalElements() +
                            accessDenied.getTotalElements() +
                            suspicious.getTotalElements();

        // Get recent events (last 50)
        Page<AuditLog> recentEvents = auditLogService.searchAuditLogs(
            null, null, null, null, null, null,
            PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "timestamp"))
        );

        // Get recent high-severity alerts (last 20)
        Page<AuditLog> recentAlerts = auditLogService.searchAuditLogs(
            null, null, null, null, null, null,
            PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "timestamp"))
        );

        Map<String, Object> stats = new HashMap<>();
        stats.put("todayEvents", todayEvents.getTotalElements());
        stats.put("failedLogins", failedLogins.getTotalElements());
        stats.put("criticalAlerts", criticalAlerts);
        stats.put("recentEvents", recentEvents.getContent());
        stats.put("recentAlerts", recentAlerts.getContent().stream()
            .filter(log -> log.getEventType() == AuditEventType.LOGIN_FAILURE ||
                          log.getEventType() == AuditEventType.ACCESS_DENIED ||
                          log.getEventType() == AuditEventType.SUSPICIOUS_ACTIVITY)
            .toList());

        return ResponseEntity.ok(stats);
    }

    /**
     * Get audit statistics (Admin only)
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getAuditStatistics(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
        Authentication authentication
    ) {
        checkAdminAccess(authentication);

        log.info("Admin '{}' retrieving audit statistics", authentication.getName());

        // For now, return a simple response
        // In a real application, you would calculate actual statistics
        Map<String, Object> statistics = new HashMap<>();
        statistics.put("message", "Audit statistics endpoint");
        statistics.put("startDate", startDate);
        statistics.put("endDate", endDate);

        return ResponseEntity.ok(statistics);
    }

    /**
     * Get recent security events (Admin only)
     */
    @GetMapping("/security-events")
    public ResponseEntity<Map<String, Object>> getSecurityEvents(
        @RequestParam(defaultValue = "24") int hours,
        Authentication authentication
    ) {
        checkAdminAccess(authentication);

        log.info("Admin '{}' retrieving security events from last {} hours",
            authentication.getName(), hours);

        LocalDateTime since = LocalDateTime.now().minusHours(hours);

        // Get recent failed login attempts
        Pageable pageable = PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "timestamp"));
        Page<AuditLog> failedLogins = auditLogService.searchAuditLogs(
            null,
            AuditEventType.LOGIN_FAILURE,
            null,
            since,
            null,
            null,
            pageable
        );

        Page<AuditLog> accessDenied = auditLogService.searchAuditLogs(
            null,
            AuditEventType.ACCESS_DENIED,
            null,
            since,
            null,
            null,
            pageable
        );

        Page<AuditLog> suspiciousActivity = auditLogService.searchAuditLogs(
            null,
            AuditEventType.SUSPICIOUS_ACTIVITY,
            null,
            since,
            null,
            null,
            pageable
        );

        Map<String, Object> securityEvents = new HashMap<>();
        securityEvents.put("failedLogins", failedLogins.getContent());
        securityEvents.put("failedLoginsCount", failedLogins.getTotalElements());
        securityEvents.put("accessDenied", accessDenied.getContent());
        securityEvents.put("accessDeniedCount", accessDenied.getTotalElements());
        securityEvents.put("suspiciousActivity", suspiciousActivity.getContent());
        securityEvents.put("suspiciousActivityCount", suspiciousActivity.getTotalElements());
        securityEvents.put("timeRange", hours + " hours");

        return ResponseEntity.ok(securityEvents);
    }

    private void checkAdminAccess(Authentication authentication) {
        User currentUser = userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new UserNotFoundException("Current user not found"));

        if (currentUser.getRole() != Role.ROLE_ADMIN) {
            auditLogService.logAccessDenied(
                authentication.getName(),
                "Audit Logs",
                "Insufficient permissions"
            );
            throw new UnauthorizedException("Only administrators can access audit logs");
        }
    }
}
