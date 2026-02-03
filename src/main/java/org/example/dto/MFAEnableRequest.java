package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request to enable MFA by verifying the initial TOTP code.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to enable MFA with TOTP verification")
public class MFAEnableRequest {

    @NotBlank(message = "TOTP code is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "TOTP code must be 6 digits")
    @Schema(description = "6-digit TOTP code from authenticator app", example = "123456", required = true)
    private String totpCode;
}
