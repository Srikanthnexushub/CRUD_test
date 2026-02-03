package org.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.entity.Role;

import java.time.LocalDateTime;

/**
 * Request object for filtering users with advanced criteria.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "User filtering criteria")
public class UserFilterRequest {

    @Schema(description = "Filter by username (partial match, case-insensitive)", example = "john")
    private String username;

    @Schema(description = "Filter by email (partial match, case-insensitive)", example = "example.com")
    private String email;

    @Schema(description = "Filter by role", example = "ROLE_USER")
    private Role role;

    @Schema(description = "Filter by MFA enabled status", example = "true")
    private Boolean mfaEnabled;

    @Schema(description = "Filter by account locked status", example = "false")
    private Boolean isAccountLocked;

    @Schema(description = "Filter users created after this date", example = "2026-01-01T00:00:00")
    private LocalDateTime createdAfter;

    @Schema(description = "Filter users created before this date", example = "2026-12-31T23:59:59")
    private LocalDateTime createdBefore;

    @Schema(description = "Page number (0-indexed)", example = "0", defaultValue = "0")
    private Integer page = 0;

    @Schema(description = "Page size", example = "20", defaultValue = "20")
    private Integer size = 20;

    @Schema(description = "Sort field", example = "createdAt", defaultValue = "id")
    private String sortBy = "id";

    @Schema(description = "Sort direction (ASC or DESC)", example = "DESC", defaultValue = "ASC")
    private String sortDirection = "ASC";
}
