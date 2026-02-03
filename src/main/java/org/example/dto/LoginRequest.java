package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login request with username and password")
public class LoginRequest {

    @NotBlank(message = "Username is required")
    @Schema(description = "Username for authentication", example = "admin", required = true)
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(description = "Password for authentication", example = "admin123", required = true, format = "password")
    private String password;

    @Schema(description = "Optional device fingerprint for device tracking", example = "fp_abc123xyz", required = false)
    private String deviceFingerprint; // Optional: client-side generated device ID
}
