package org.example.repository;

import org.example.entity.ThreatIntelligence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Threat Intelligence Repository
 */
@Repository
public interface ThreatIntelligenceRepository extends JpaRepository<ThreatIntelligence, Long> {

    /**
     * Find threat intelligence by IP address
     */
    Optional<ThreatIntelligence> findByIpAddress(String ipAddress);

    /**
     * Find all IPs with risk score above threshold
     */
    @Query("SELECT t FROM ThreatIntelligence t WHERE t.riskScore >= :threshold ORDER BY t.riskScore DESC")
    List<ThreatIntelligence> findByRiskScoreGreaterThanEqual(@Param("threshold") Integer threshold);

    /**
     * Find all blacklisted IPs
     */
    List<ThreatIntelligence> findByIsBlacklistedTrue();

    /**
     * Find all IPs with specific threat type
     */
    List<ThreatIntelligence> findByThreatType(String threatType);

    /**
     * Find all IPs from specific country
     */
    List<ThreatIntelligence> findByCountryCode(String countryCode);

    /**
     * Find all Tor exit nodes
     */
    List<ThreatIntelligence> findByIsTorTrue();

    /**
     * Find all VPN/Proxy IPs
     */
    @Query("SELECT t FROM ThreatIntelligence t WHERE t.isVpn = true OR t.isProxy = true")
    List<ThreatIntelligence> findAllVpnAndProxyIps();

    /**
     * Find IPs with recent suspicious activity
     */
    @Query("SELECT t FROM ThreatIntelligence t WHERE t.lastSeen > :since ORDER BY t.suspiciousActivityCount DESC")
    List<ThreatIntelligence> findRecentSuspiciousActivity(@Param("since") LocalDateTime since);

    /**
     * Delete expired threat intelligence records
     */
    @Modifying
    @Query("DELETE FROM ThreatIntelligence t WHERE t.expiresAt < :now")
    void deleteExpired(@Param("now") LocalDateTime now);

    /**
     * Count IPs above risk threshold
     */
    @Query("SELECT COUNT(t) FROM ThreatIntelligence t WHERE t.riskScore >= :threshold")
    long countHighRiskIps(@Param("threshold") Integer threshold);

    /**
     * Get top risky IPs
     */
    @Query("SELECT t FROM ThreatIntelligence t ORDER BY t.riskScore DESC, t.suspiciousActivityCount DESC")
    List<ThreatIntelligence> findTopRiskyIps();
}
