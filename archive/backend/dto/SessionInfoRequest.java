package org.example.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO containing device fingerprint and session information from frontend
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionInfoRequest {
    private String deviceFingerprint;
    private String screenResolution;
    private String timezone;
    private String language;
    private Integer colorDepth;
    private String platform;
}
