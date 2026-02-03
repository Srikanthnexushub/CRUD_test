package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to verify MFA code during login.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to verify MFA code (TOTP or backup code)")
public class MFAVerifyRequest {

    @NotBlank(message = "Username is required")
    @Schema(description = "Username", example = "admin", required = true)
    private String username;

    @NotBlank(message = "Code is required")
    @Schema(description = "6-digit TOTP code or 8-character backup code", example = "123456", required = true)
    private String code;

    @Schema(description = "Whether to trust this device for 30 days", example = "false")
    private Boolean trustDevice = false;

    @Schema(description = "Device name for identification", example = "Chrome on MacBook Pro")
    private String deviceName;
}
