package org.example.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.IpRiskAssessment;
import org.example.dto.ThreatIntelligenceResponse;
import org.example.entity.ThreatIntelligence;
import org.example.enums.AuditAction;
import org.example.repository.ThreatIntelligenceRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Threat Intelligence Service Implementation
 * Provides IP risk assessment, threat detection, and security intelligence
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ThreatIntelligenceServiceImpl implements ThreatIntelligenceService {

    private final ThreatIntelligenceRepository threatRepository;
    private final AuditLogService auditLogService;

    // Risk scoring weights
    private static final int RISK_WEIGHT_FAILED_LOGIN = 5;
    private static final int RISK_WEIGHT_SUSPICIOUS_ACTIVITY = 10;
    private static final int RISK_WEIGHT_TOR = 50;
    private static final int RISK_WEIGHT_PROXY = 30;
    private static final int RISK_WEIGHT_VPN = 20;
    private static final int RISK_WEIGHT_DATACENTER = 15;

    @Override
    @Transactional
    public IpRiskAssessment assessIpRisk(String ipAddress) {
        ThreatIntelligence threat = threatRepository.findByIpAddress(ipAddress)
            .orElseGet(() -> createNewThreatRecord(ipAddress));

        // Calculate risk score
        int riskScore = calculateRiskScore(threat);
        threat.setRiskScore(riskScore);
        threatRepository.save(threat);

        return IpRiskAssessment.builder()
            .ipAddress(ipAddress)
            .riskScore(riskScore)
            .riskLevel(threat.getRiskLevel())
            .shouldBlock(threat.shouldBlock())
            .isTor(threat.getIsTor())
            .isVpn(threat.getIsVpn())
            .isProxy(threat.getIsProxy())
            .isDatacenter(threat.getIsDatacenter())
            .isBlacklisted(threat.getIsBlacklisted())
            .failedLoginCount(threat.getFailedLoginCount())
            .suspiciousActivityCount(threat.getSuspiciousActivityCount())
            .countryCode(threat.getCountryCode())
            .threatType(threat.getThreatType())
            .build();
    }

    @Override
    public boolean shouldBlockIp(String ipAddress) {
        return threatRepository.findByIpAddress(ipAddress)
            .map(ThreatIntelligence::shouldBlock)
            .orElse(false);
    }

    @Override
    @Transactional
    public void recordFailedLogin(String ipAddress, String userAgent) {
        ThreatIntelligence threat = threatRepository.findByIpAddress(ipAddress)
            .orElseGet(() -> createNewThreatRecord(ipAddress));

        threat.recordFailedLogin();
        threatRepository.save(threat);

        // Check if IP should be blacklisted
        if (threat.getFailedLoginCount() >= 10 && !threat.getIsBlacklisted()) {
            blacklistIp(ipAddress, "Excessive failed login attempts: " + threat.getFailedLoginCount());
        }

        log.warn("Failed login recorded for IP: {} (count: {}, risk: {})",
            ipAddress, threat.getFailedLoginCount(), threat.getRiskScore());
    }

    @Override
    @Transactional
    public void recordSuspiciousActivity(String ipAddress, String activityType, String details) {
        ThreatIntelligence threat = threatRepository.findByIpAddress(ipAddress)
            .orElseGet(() -> createNewThreatRecord(ipAddress));

        threat.recordSuspiciousActivity();
        threat.setThreatType(activityType);
        threat.setNotes(details);
        threatRepository.save(threat);

        // Audit log
        auditLogService.log(
            AuditAction.SUSPICIOUS_ACTIVITY,
            null,
            "ThreatIntelligence",
            threat.getId(),
            ipAddress,
            null,
            String.format("Activity: %s, Details: %s", activityType, details)
        );

        log.warn("Suspicious activity recorded for IP: {} - Type: {}, Risk: {}",
            ipAddress, activityType, threat.getRiskScore());
    }

    @Override
    @Transactional
    public void blacklistIp(String ipAddress, String reason) {
        ThreatIntelligence threat = threatRepository.findByIpAddress(ipAddress)
            .orElseGet(() -> createNewThreatRecord(ipAddress));

        threat.setIsBlacklisted(true);
        threat.setRiskScore(100);
        threat.setNotes(reason);
        threat.setThreatCategory("BLACKLISTED");
        threatRepository.save(threat);

        // Audit log
        auditLogService.log(
            AuditAction.IP_BLOCKED,
            null,
            "ThreatIntelligence",
            threat.getId(),
            ipAddress,
            null,
            "IP blacklisted: " + reason
        );

        log.warn("IP blacklisted: {} - Reason: {}", ipAddress, reason);
    }

    @Override
    @Transactional
    public void whitelistIp(String ipAddress) {
        threatRepository.findByIpAddress(ipAddress).ifPresent(threat -> {
            threat.setIsBlacklisted(false);
            threat.setRiskScore(0);
            threat.setFailedLoginCount(0);
            threat.setSuspiciousActivityCount(0);
            threat.setThreatCategory("WHITELISTED");
            threatRepository.save(threat);

            log.info("IP whitelisted: {}", ipAddress);
        });
    }

    @Override
    public ThreatIntelligence getThreatIntelligence(String ipAddress) {
        return threatRepository.findByIpAddress(ipAddress).orElse(null);
    }

    @Override
    public List<ThreatIntelligenceResponse> getHighRiskIps(Integer riskThreshold) {
        return threatRepository.findByRiskScoreGreaterThanEqual(riskThreshold)
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Override
    public List<ThreatIntelligenceResponse> getBlacklistedIps() {
        return threatRepository.findByIsBlacklistedTrue()
            .stream()
            .map(this::toResponse)
            .collect(Collectors.toList());
    }

    @Override
    @CircuitBreaker(name = "ipLookup", fallbackMethod = "enrichIpFallback")
    public void enrichIpIntelligence(String ipAddress) {
        // This would integrate with external IP intelligence APIs
        // Examples: AbuseIPDB, IPQualityScore, MaxMind GeoIP, etc.

        ThreatIntelligence threat = threatRepository.findByIpAddress(ipAddress)
            .orElseGet(() -> createNewThreatRecord(ipAddress));

        // TODO: Integrate with external API
        // For now, implement basic detection
        threat.setSource("INTERNAL");

        // Basic VPN/Proxy detection (simplified)
        if (isKnownVpnRange(ipAddress)) {
            threat.setIsVpn(true);
            threat.setRiskScore(Math.min(100, threat.getRiskScore() + RISK_WEIGHT_VPN));
        }

        // Check if datacenter IP (AWS, Azure, GCP, etc.)
        if (isDatacenterIp(ipAddress)) {
            threat.setIsDatacenter(true);
            threat.setRiskScore(Math.min(100, threat.getRiskScore() + RISK_WEIGHT_DATACENTER));
        }

        threatRepository.save(threat);
    }

    @Override
    @Transactional
    @Scheduled(cron = "${app.scheduled.cleanup-threat-intelligence:0 0 4 * * *}") // 4 AM daily
    public void cleanupExpiredRecords() {
        try {
            threatRepository.deleteExpired(LocalDateTime.now());
            log.info("Expired threat intelligence records cleaned up");
        } catch (Exception e) {
            log.error("Failed to cleanup expired threat intelligence records", e);
        }
    }

    @Override
    public ThreatStatistics getThreatStatistics() {
        long totalThreats = threatRepository.count();
        long criticalThreats = threatRepository.countHighRiskIps(80);
        long highRiskThreats = threatRepository.countHighRiskIps(60);
        long blacklistedCount = threatRepository.findByIsBlacklistedTrue().size();
        long torExitNodes = threatRepository.findByIsTorTrue().size();

        return ThreatStatistics.builder()
            .totalThreats(totalThreats)
            .criticalRiskCount(criticalThreats)
            .highRiskCount(highRiskThreats)
            .blacklistedCount(blacklistedCount)
            .torExitNodeCount(torExitNodes)
            .build();
    }

    /**
     * Calculate risk score based on multiple factors
     */
    private int calculateRiskScore(ThreatIntelligence threat) {
        int score = 0;

        // Base score from existing risk score
        score = threat.getRiskScore();

        // Add weights for different threat indicators
        if (threat.getIsTor()) score += RISK_WEIGHT_TOR;
        if (threat.getIsProxy()) score += RISK_WEIGHT_PROXY;
        if (threat.getIsVpn()) score += RISK_WEIGHT_VPN;
        if (threat.getIsDatacenter()) score += RISK_WEIGHT_DATACENTER;

        // Failed login attempts
        score += threat.getFailedLoginCount() * RISK_WEIGHT_FAILED_LOGIN;

        // Suspicious activities
        score += threat.getSuspiciousActivityCount() * RISK_WEIGHT_SUSPICIOUS_ACTIVITY;

        // Blacklisted IPs get maximum score
        if (threat.getIsBlacklisted()) score = 100;

        // Cap at 100
        return Math.min(100, score);
    }

    /**
     * Create new threat intelligence record
     */
    private ThreatIntelligence createNewThreatRecord(String ipAddress) {
        return ThreatIntelligence.builder()
            .ipAddress(ipAddress)
            .riskScore(0)
            .failedLoginCount(0)
            .suspiciousActivityCount(0)
            .isBlacklisted(false)
            .isTor(false)
            .isVpn(false)
            .isProxy(false)
            .isDatacenter(false)
            .source("INTERNAL")
            .build();
    }

    /**
     * Convert entity to response DTO
     */
    private ThreatIntelligenceResponse toResponse(ThreatIntelligence threat) {
        return ThreatIntelligenceResponse.builder()
            .id(threat.getId())
            .ipAddress(threat.getIpAddress())
            .riskScore(threat.getRiskScore())
            .riskLevel(threat.getRiskLevel())
            .threatType(threat.getThreatType())
            .threatCategory(threat.getThreatCategory())
            .countryCode(threat.getCountryCode())
            .isVpn(threat.getIsVpn())
            .isProxy(threat.getIsProxy())
            .isTor(threat.getIsTor())
            .isDatacenter(threat.getIsDatacenter())
            .isBlacklisted(threat.getIsBlacklisted())
            .failedLoginCount(threat.getFailedLoginCount())
            .suspiciousActivityCount(threat.getSuspiciousActivityCount())
            .lastSeen(threat.getLastSeen())
            .createdAt(threat.getCreatedAt())
            .notes(threat.getNotes())
            .build();
    }

    /**
     * Fallback method for IP enrichment
     */
    private void enrichIpFallback(String ipAddress, Exception e) {
        log.warn("IP enrichment fallback activated for IP: {}. Error: {}", ipAddress, e.getMessage());
    }

    /**
     * Check if IP is in known VPN range (simplified)
     */
    private boolean isKnownVpnRange(String ipAddress) {
        // TODO: Implement proper VPN detection using external API
        // This is a placeholder
        return false;
    }

    /**
     * Check if IP belongs to datacenter (simplified)
     */
    private boolean isDatacenterIp(String ipAddress) {
        // TODO: Implement datacenter detection
        // Common ranges: AWS, Azure, GCP, DigitalOcean, etc.
        return ipAddress.startsWith("54.") ||  // AWS EC2
               ipAddress.startsWith("52.") ||  // AWS
               ipAddress.startsWith("13.") ||  // Azure
               ipAddress.startsWith("20.") ||  // Azure
               ipAddress.startsWith("35.") ||  // GCP
               ipAddress.startsWith("34.");    // GCP
    }
}
