package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.service.RateLimitService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rate-limit")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ROLE_ADMIN')")
public class RateLimitController {

    private final RateLimitService rateLimitService;

    @GetMapping("/stats")
    public ResponseEntity<?> getStatistics() {
        try {
            Map<String, Object> stats = rateLimitService.getStatistics();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("stats", stats);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get rate limit statistics", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get statistics: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/violations")
    public ResponseEntity<?> getRecentViolations(
            @RequestParam(defaultValue = "50") int limit) {

        try {
            List<Map<String, Object>> violations = rateLimitService.getRecentViolations(limit);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("violations", violations);
            response.put("count", violations.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get violations", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get violations: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/whitelist")
    public ResponseEntity<?> getWhitelist() {
        try {
            List<Map<String, Object>> whitelist = rateLimitService.getWhitelist();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("whitelist", whitelist);
            response.put("count", whitelist.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to get whitelist", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to get whitelist: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/whitelist/ip")
    public ResponseEntity<?> addIpToWhitelist(@RequestBody Map<String, Object> request, Principal principal) {
        String ipAddress = (String) request.get("ipAddress");
        String reason = (String) request.getOrDefault("reason", "Added by administrator");
        Integer expiresInDays = (Integer) request.get("expiresInDays");

        try {
            rateLimitService.addToWhitelist(ipAddress, reason, principal.getName(), expiresInDays);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "IP added to whitelist");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to add IP to whitelist", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to add IP to whitelist: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/whitelist/user/{userId}")
    public ResponseEntity<?> addUserToWhitelist(@PathVariable Long userId,
                                                 @RequestBody Map<String, Object> request,
                                                 Principal principal) {
        String reason = (String) request.getOrDefault("reason", "Added by administrator");
        Integer expiresInDays = (Integer) request.get("expiresInDays");

        try {
            rateLimitService.addUserToWhitelist(userId, reason, principal.getName(), expiresInDays);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "User added to whitelist");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to add user to whitelist", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to add user to whitelist: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    @DeleteMapping("/whitelist/{whitelistId}")
    public ResponseEntity<?> removeFromWhitelist(@PathVariable Long whitelistId) {
        try {
            rateLimitService.removeFromWhitelist(whitelistId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Entry removed from whitelist");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to remove from whitelist", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to remove from whitelist: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }
}
