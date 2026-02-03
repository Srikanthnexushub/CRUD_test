package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response containing MFA setup information (secret, QR code, backup codes).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "MFA setup response with QR code and backup codes")
public class MFASetupResponse {

    @Schema(description = "Base32-encoded TOTP secret", example = "JBSWY3DPEHPK3PXP")
    private String secret;

    @Schema(description = "OTPAuth URI for QR code generation",
            example = "otpauth://totp/CRUDTest:admin?secret=JBSWY3DPEHPK3PXP&issuer=CRUDTest")
    private String otpAuthUri;

    @Schema(description = "Base64-encoded QR code image (PNG format)")
    private String qrCodeBase64;

    @Schema(description = "Array of one-time backup codes")
    private String[] backupCodes;

    @Schema(description = "Instructions for the user")
    private String instructions;
}
