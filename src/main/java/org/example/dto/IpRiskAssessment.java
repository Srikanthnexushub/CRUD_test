package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * IP Risk Assessment DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IpRiskAssessment {

    private String ipAddress;
    private Integer riskScore;
    private String riskLevel;
    private Boolean shouldBlock;
    private Boolean isTor;
    private Boolean isVpn;
    private Boolean isProxy;
    private Boolean isDatacenter;
    private Boolean isBlacklisted;
    private Integer failedLoginCount;
    private Integer suspiciousActivityCount;
    private String countryCode;
    private String threatType;
}
