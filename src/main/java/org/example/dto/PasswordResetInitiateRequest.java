package org.example.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Password Reset Initiate Request DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetInitiateRequest {

    @NotBlank(message = "Email or username is required")
    private String email;
}
