package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Threat Intelligence Response DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThreatIntelligenceResponse {

    private Long id;
    private String ipAddress;
    private Integer riskScore;
    private String riskLevel;
    private String threatType;
    private String threatCategory;
    private String countryCode;
    private Boolean isVpn;
    private Boolean isProxy;
    private Boolean isTor;
    private Boolean isDatacenter;
    private Boolean isBlacklisted;
    private Integer failedLoginCount;
    private Integer suspiciousActivityCount;
    private LocalDateTime lastSeen;
    private LocalDateTime createdAt;
    private String notes;
}
