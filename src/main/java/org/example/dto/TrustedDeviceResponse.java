package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Response containing trusted device information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Trusted device information")
public class TrustedDeviceResponse {

    @Schema(description = "Device ID")
    private Long id;

    @Schema(description = "Device name", example = "Chrome on MacBook Pro")
    private String deviceName;

    @Schema(description = "IP address when device was trusted")
    private String ipAddress;

    @Schema(description = "When device was first trusted")
    private LocalDateTime createdAt;

    @Schema(description = "When device was last used")
    private LocalDateTime lastUsedAt;

    @Schema(description = "When trust expires")
    private LocalDateTime trustedUntil;
}
