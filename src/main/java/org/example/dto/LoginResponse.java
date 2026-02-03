package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.entity.Role;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login response containing JWT token and user information")
public class LoginResponse {

    @Schema(description = "JWT access token (valid for 1 hour)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String token;

    @Schema(description = "Refresh token (valid for 7 days)", example = "550e8400-e29b-41d4-a716-446655440000")
    private String refreshToken;

    @Schema(description = "Token type", example = "Bearer", defaultValue = "Bearer")
    private String type = "Bearer";

    @Schema(description = "User ID", example = "1")
    private Long id;

    @Schema(description = "Username", example = "admin")
    private String username;

    @Schema(description = "Email address", example = "admin@crudtest.com")
    private String email;

    @Schema(description = "User role", example = "ROLE_ADMIN", allowableValues = {"ROLE_USER", "ROLE_ADMIN"})
    private Role role;

    // MFA fields
    @Schema(description = "Whether MFA verification is required", example = "false", defaultValue = "false")
    private Boolean mfaRequired = false;

    @Schema(description = "Temporary token for MFA verification (if MFA enabled)", example = "temp_token_xyz")
    private String tempToken;

    @Schema(description = "Whether MFA is enabled for this user", example = "false", defaultValue = "false")
    private Boolean mfaEnabled = false;

    // Account lock fields
    @Schema(description = "Whether the account is locked", example = "false", defaultValue = "false")
    private Boolean accountLocked = false;

    @Schema(description = "Account lock details (if locked)", example = "{\"reason\": \"Too many failed attempts\", \"lockedUntil\": \"2026-02-03T12:00:00\"}")
    private Map<String, Object> lockDetails;

    public LoginResponse(String token, Long id, String username, String email, Role role) {
        this.token = token;
        this.type = "Bearer";
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.mfaRequired = false;
        this.mfaEnabled = false;
        this.accountLocked = false;
    }
}
