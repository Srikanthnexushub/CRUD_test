package org.example.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Threat Statistics DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThreatStatistics {

    private Long totalThreats;
    private Long criticalRiskCount;
    private Long highRiskCount;
    private Long blacklistedCount;
    private Long torExitNodeCount;
}
