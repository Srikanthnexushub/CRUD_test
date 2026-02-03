package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.example.dto.IpRiskAssessment;
import org.example.dto.ThreatIntelligenceResponse;
import org.example.service.ThreatIntelligenceService;
import org.example.service.ThreatStatistics;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Threat Intelligence Controller
 * Admin-only endpoints for managing security threats and IP intelligence
 */
@RestController
@RequestMapping("/api/v1/threat-intelligence")
@RequiredArgsConstructor
@Tag(name = "Threat Intelligence", description = "Security threat management and IP intelligence")
@SecurityRequirement(name = "bearer-jwt")
public class ThreatIntelligenceController {

    private final ThreatIntelligenceService threatIntelligenceService;

    @GetMapping("/assess/{ipAddress}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assess IP risk", description = "Get risk assessment for an IP address")
    public ResponseEntity<IpRiskAssessment> assessIpRisk(@PathVariable String ipAddress) {
        IpRiskAssessment assessment = threatIntelligenceService.assessIpRisk(ipAddress);
        return ResponseEntity.ok(assessment);
    }

    @GetMapping("/check/{ipAddress}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Check if IP should be blocked", description = "Quick check if IP is dangerous")
    public ResponseEntity<Map<String, Boolean>> checkIpShouldBlock(@PathVariable String ipAddress) {
        boolean shouldBlock = threatIntelligenceService.shouldBlockIp(ipAddress);
        return ResponseEntity.ok(Map.of(
            "ipAddress", ipAddress,
            "shouldBlock", shouldBlock
        ));
    }

    @GetMapping("/high-risk")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get high-risk IPs", description = "List all IPs above risk threshold")
    public ResponseEntity<List<ThreatIntelligenceResponse>> getHighRiskIps(
            @RequestParam(defaultValue = "60") Integer riskThreshold) {
        List<ThreatIntelligenceResponse> threats = threatIntelligenceService.getHighRiskIps(riskThreshold);
        return ResponseEntity.ok(threats);
    }

    @GetMapping("/blacklist")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get blacklisted IPs", description = "List all blacklisted IP addresses")
    public ResponseEntity<List<ThreatIntelligenceResponse>> getBlacklistedIps() {
        List<ThreatIntelligenceResponse> threats = threatIntelligenceService.getBlacklistedIps();
        return ResponseEntity.ok(threats);
    }

    @PostMapping("/blacklist/{ipAddress}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Blacklist IP", description = "Add IP to blacklist")
    public ResponseEntity<Map<String, String>> blacklistIp(
            @PathVariable String ipAddress,
            @RequestParam String reason) {
        threatIntelligenceService.blacklistIp(ipAddress, reason);
        return ResponseEntity.ok(Map.of(
            "message", "IP blacklisted successfully",
            "ipAddress", ipAddress,
            "reason", reason
        ));
    }

    @DeleteMapping("/blacklist/{ipAddress}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Whitelist IP", description = "Remove IP from blacklist")
    public ResponseEntity<Map<String, String>> whitelistIp(@PathVariable String ipAddress) {
        threatIntelligenceService.whitelistIp(ipAddress);
        return ResponseEntity.ok(Map.of(
            "message", "IP whitelisted successfully",
            "ipAddress", ipAddress
        ));
    }

    @PostMapping("/enrich/{ipAddress}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Enrich IP intelligence", description = "Lookup IP geolocation and threat data")
    public ResponseEntity<Map<String, String>> enrichIpIntelligence(@PathVariable String ipAddress) {
        threatIntelligenceService.enrichIpIntelligence(ipAddress);
        return ResponseEntity.ok(Map.of(
            "message", "IP intelligence enriched",
            "ipAddress", ipAddress
        ));
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get threat statistics", description = "Get overall threat intelligence statistics")
    public ResponseEntity<ThreatStatistics> getThreatStatistics() {
        ThreatStatistics stats = threatIntelligenceService.getThreatStatistics();
        return ResponseEntity.ok(stats);
    }

    @PostMapping("/record-suspicious")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Record suspicious activity", description = "Manually record suspicious activity for an IP")
    public ResponseEntity<Map<String, String>> recordSuspiciousActivity(
            @RequestParam String ipAddress,
            @RequestParam String activityType,
            @RequestParam(required = false) String details) {
        threatIntelligenceService.recordSuspiciousActivity(ipAddress, activityType, details);
        return ResponseEntity.ok(Map.of(
            "message", "Suspicious activity recorded",
            "ipAddress", ipAddress,
            "activityType", activityType
        ));
    }
}
