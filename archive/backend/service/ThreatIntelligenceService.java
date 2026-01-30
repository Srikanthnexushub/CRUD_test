package org.example.service;

import org.example.entity.ThreatAssessment;
import org.example.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ThreatIntelligenceService {

    /**
     * Assess threat level for a login attempt (async)
     */
    void assessThreat(User user, String ipAddress, String deviceFingerprint, String userAgent);

    /**
     * Calculate threat score based on various factors
     */
    int calculateThreatScore(Map<String, Object> factors);

    /**
     * Lock user account due to high risk
     */
    void lockAccount(Long userId, String reason, int minutes);

    /**
     * Unlock user account (admin action)
     */
    void unlockAccount(Long userId);

    /**
     * Check if account should be automatically unlocked
     */
    void checkAndUnlockExpiredLocks();

    /**
     * Get threat assessments for a user
     */
    Page<ThreatAssessment> getThreatAssessments(Long userId, Pageable pageable);

    /**
     * Get high-risk assessments (admin)
     */
    Page<ThreatAssessment> getHighRiskAssessments(int minScore, Pageable pageable);

    /**
     * Get threat assessments in date range
     */
    List<ThreatAssessment> getThreatAssessmentsByDateRange(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Get IP reputation from cache or external API
     */
    Map<String, Object> getIPReputation(String ipAddress);

    /**
     * Clear IP reputation cache
     */
    void clearIPReputationCache();
}
