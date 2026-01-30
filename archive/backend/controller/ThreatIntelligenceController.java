package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.ThreatAssessment;
import org.example.service.ThreatIntelligenceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/threat")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class ThreatIntelligenceController {

    private final ThreatIntelligenceService threatService;

    @GetMapping("/assessments")
    public ResponseEntity<?> getAllAssessments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer minScore) {

        Page<ThreatAssessment> assessments;

        if (minScore != null) {
            assessments = threatService.getHighRiskAssessments(minScore,
                    PageRequest.of(page, size, Sort.by("assessedAt").descending()));
        } else {
            assessments = threatService.getHighRiskAssessments(0,
                    PageRequest.of(page, size, Sort.by("assessedAt").descending()));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("assessments", assessments.getContent());
        response.put("totalElements", assessments.getTotalElements());
        response.put("totalPages", assessments.getTotalPages());
        response.put("currentPage", assessments.getNumber());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/assessments/user/{userId}")
    public ResponseEntity<?> getUserAssessments(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Page<ThreatAssessment> assessments = threatService.getThreatAssessments(userId,
                PageRequest.of(page, size, Sort.by("assessedAt").descending()));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("assessments", assessments.getContent());
        response.put("totalElements", assessments.getTotalElements());
        response.put("totalPages", assessments.getTotalPages());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/assessments/date-range")
    public ResponseEntity<?> getAssessmentsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        List<ThreatAssessment> assessments = threatService.getThreatAssessmentsByDateRange(startDate, endDate);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("assessments", assessments);
        response.put("count", assessments.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/ip-reputation/{ipAddress}")
    public ResponseEntity<?> getIPReputation(@PathVariable String ipAddress) {
        try {
            Map<String, Object> reputation = threatService.getIPReputation(ipAddress);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("ipAddress", ipAddress);
            response.put("reputation", reputation);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get IP reputation", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get IP reputation: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/unlock-account/{userId}")
    public ResponseEntity<?> unlockAccount(@PathVariable Long userId) {
        try {
            threatService.unlockAccount(userId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Account unlocked successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to unlock account", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to unlock account: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/lock-account/{userId}")
    public ResponseEntity<?> lockAccount(
            @PathVariable Long userId,
            @RequestBody Map<String, Object> request) {

        String reason = (String) request.getOrDefault("reason", "Manually locked by administrator");
        Integer minutes = (Integer) request.getOrDefault("minutes", 30);

        try {
            threatService.lockAccount(userId, reason, minutes);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Account locked successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to lock account", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to lock account: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    @DeleteMapping("/cache/clear")
    public ResponseEntity<?> clearCache() {
        try {
            threatService.clearIPReputationCache();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "IP reputation cache cleared");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to clear cache", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to clear cache: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/stats")
    public ResponseEntity<?> getThreatStats() {
        try {
            LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);
            LocalDateTime last7Days = LocalDateTime.now().minusDays(7);

            List<ThreatAssessment> last24HoursData = threatService.getThreatAssessmentsByDateRange(
                    last24Hours, LocalDateTime.now());
            List<ThreatAssessment> last7DaysData = threatService.getThreatAssessmentsByDateRange(
                    last7Days, LocalDateTime.now());

            Map<String, Object> stats = new HashMap<>();
            stats.put("total24Hours", last24HoursData.size());
            stats.put("total7Days", last7DaysData.size());

            stats.put("highRisk24Hours", last24HoursData.stream()
                    .filter(a -> a.getRiskScore() >= 60).count());
            stats.put("highRisk7Days", last7DaysData.stream()
                    .filter(a -> a.getRiskScore() >= 60).count());

            stats.put("accountsLocked24Hours", last24HoursData.stream()
                    .filter(a -> "ACCOUNT_LOCKED".equals(a.getActionTaken())).count());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("stats", stats);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get threat stats", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get stats: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }
}
