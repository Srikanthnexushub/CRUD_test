package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.entity.Role;
import org.example.entity.User;

import java.time.LocalDateTime;

/**
 * User response DTO without sensitive information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User information response")
public class UserResponse {

    @Schema(description = "User ID", example = "1")
    private Long id;

    @Schema(description = "Username", example = "john_doe")
    private String username;

    @Schema(description = "Email address", example = "john@example.com")
    private String email;

    @Schema(description = "User role", example = "ROLE_USER")
    private Role role;

    @Schema(description = "Whether MFA is enabled", example = "false")
    private Boolean mfaEnabled;

    @Schema(description = "Whether account is locked", example = "false")
    private Boolean isAccountLocked;

    @Schema(description = "Account locked until (if locked)", example = "2026-02-03T15:30:00")
    private LocalDateTime accountLockedUntil;

    @Schema(description = "Reason for account lock", example = "Too many failed login attempts")
    private String lockReason;

    @Schema(description = "When user was created", example = "2026-01-15T10:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "When user was last updated", example = "2026-02-01T14:30:00")
    private LocalDateTime updatedAt;

    /**
     * Convert User entity to UserResponse DTO.
     */
    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .mfaEnabled(user.getMfaEnabled())
                .isAccountLocked(user.getIsAccountLocked())
                .accountLockedUntil(user.getAccountLockedUntil())
                .lockReason(user.getLockReason())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
