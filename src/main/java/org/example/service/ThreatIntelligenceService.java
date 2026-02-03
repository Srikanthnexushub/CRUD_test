package org.example.service;

import org.example.dto.IpRiskAssessment;
import org.example.dto.ThreatIntelligenceResponse;
import org.example.entity.ThreatIntelligence;

import java.util.List;

/**
 * Threat Intelligence Service Interface
 */
public interface ThreatIntelligenceService {

    /**
     * Assess risk for an IP address
     */
    IpRiskAssessment assessIpRisk(String ipAddress);

    /**
     * Check if IP should be blocked
     */
    boolean shouldBlockIp(String ipAddress);

    /**
     * Record failed login attempt for IP
     */
    void recordFailedLogin(String ipAddress, String userAgent);

    /**
     * Record suspicious activity for IP
     */
    void recordSuspiciousActivity(String ipAddress, String activityType, String details);

    /**
     * Blacklist an IP address
     */
    void blacklistIp(String ipAddress, String reason);

    /**
     * Whitelist an IP address (remove from blacklist)
     */
    void whitelistIp(String ipAddress);

    /**
     * Get threat intelligence for IP
     */
    ThreatIntelligence getThreatIntelligence(String ipAddress);

    /**
     * Get all high-risk IPs
     */
    List<ThreatIntelligenceResponse> getHighRiskIps(Integer riskThreshold);

    /**
     * Get all blacklisted IPs
     */
    List<ThreatIntelligenceResponse> getBlacklistedIps();

    /**
     * Lookup IP geolocation and VPN/Proxy status
     */
    void enrichIpIntelligence(String ipAddress);

    /**
     * Clean up expired threat intelligence records
     */
    void cleanupExpiredRecords();

    /**
     * Get threat statistics
     */
    ThreatStatistics getThreatStatistics();
}
