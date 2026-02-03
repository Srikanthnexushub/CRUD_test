package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.entity.Role;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User update request. All fields are optional (partial update supported).")
public class UserUpdateRequest {

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers, and underscores")
    @Schema(
            description = "New username (3-50 characters, alphanumeric and underscores only)",
            example = "john_doe",
            minLength = 3,
            maxLength = 50,
            pattern = "^[a-zA-Z0-9_]+$"
    )
    private String username;

    @Email(message = "Email must be valid")
    @Size(max = 100, message = "Email must not exceed 100 characters")
    @Schema(
            description = "New email address",
            example = "john.doe@example.com",
            format = "email",
            maxLength = 100
    )
    private String email;

    @Size(min = 8, message = "Password must be at least 8 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).*$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, and one digit")
    @Schema(
            description = "New password (min 8 chars, must contain uppercase, lowercase, and digit)",
            example = "NewP@ssw0rd",
            format = "password",
            minLength = 8
    )
    private String password;

    @Schema(
            description = "New role (ADMIN only can modify). Options: ROLE_USER, ROLE_ADMIN",
            example = "ROLE_ADMIN",
            allowableValues = {"ROLE_USER", "ROLE_ADMIN"}
    )
    private Role role; // Only admins can change roles
}
