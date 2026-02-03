package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.PageResponse;
import org.example.dto.UserFilterRequest;
import org.example.dto.UserResponse;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.repository.UserRepository;
import org.example.specification.UserSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * REST Controller for advanced user management with pagination, filtering, and sorting.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management (Advanced)", description = "Advanced user management with pagination, filtering, and sorting")
@SecurityRequirement(name = "bearerAuth")
public class UserManagementController {

    private final UserRepository userRepository;

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Search users with filters",
            description = """
                    Advanced user search with pagination, filtering, and sorting.

                    **Filters:**
                    - Username (partial match, case-insensitive)
                    - Email (partial match, case-insensitive)
                    - Role (exact match)
                    - MFA enabled status
                    - Account locked status
                    - Created date range

                    **Sorting:**
                    - Supported fields: id, username, email, createdAt, updatedAt
                    - Direction: ASC or DESC

                    **Pagination:**
                    - Page: 0-indexed
                    - Size: 1-100 items per page

                    **Example:**
                    ```
                    GET /api/users/search?username=john&role=ROLE_USER&page=0&size=20&sortBy=createdAt&sortDirection=DESC
                    ```
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Users retrieved successfully",
                    content = @Content(schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid filter parameters"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin only")
    })
    public ResponseEntity<PageResponse<UserResponse>> searchUsers(
            @Parameter(description = "Username filter (partial match)") @RequestParam(required = false) String username,
            @Parameter(description = "Email filter (partial match)") @RequestParam(required = false) String email,
            @Parameter(description = "Role filter") @RequestParam(required = false) Role role,
            @Parameter(description = "MFA enabled filter") @RequestParam(required = false) Boolean mfaEnabled,
            @Parameter(description = "Account locked filter") @RequestParam(required = false) Boolean isAccountLocked,
            @Parameter(description = "Created after date") @RequestParam(required = false) LocalDateTime createdAfter,
            @Parameter(description = "Created before date") @RequestParam(required = false) LocalDateTime createdBefore,
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field") @RequestParam(defaultValue = "id") String sortBy,
            @Parameter(description = "Sort direction (ASC/DESC)") @RequestParam(defaultValue = "ASC") String sortDirection
    ) {
        log.info("Searching users with filters - username: {}, email: {}, role: {}, page: {}, size: {}, sort: {} {}",
                 username, email, role, page, size, sortBy, sortDirection);

        // Validate and limit page size
        size = Math.min(size, 100);
        size = Math.max(size, 1);

        // Build filter request
        UserFilterRequest filter = UserFilterRequest.builder()
                .username(username)
                .email(email)
                .role(role)
                .mfaEnabled(mfaEnabled)
                .isAccountLocked(isAccountLocked)
                .createdAfter(createdAfter)
                .createdBefore(createdBefore)
                .page(page)
                .size(size)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();

        // Build specification
        Specification<User> spec = UserSpecification.withFilters(filter);

        // Build pageable with sorting
        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        // Execute query
        Page<User> userPage = userRepository.findAll(spec, pageable);

        // Convert to DTOs
        Page<UserResponse> responsePage = userPage.map(UserResponse::from);

        log.info("Found {} users (page {} of {})", userPage.getTotalElements(), page + 1, userPage.getTotalPages());

        return ResponseEntity.ok(PageResponse.from(responsePage));
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get active users",
            description = "Retrieve paginated list of active (non-locked) users"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Active users retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin only")
    })
    public ResponseEntity<PageResponse<UserResponse>> getActiveUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection
    ) {
        log.info("Fetching active users - page: {}, size: {}", page, size);

        size = Math.min(size, 100);
        Sort.Direction direction = "DESC".equalsIgnoreCase(sortDirection) ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<User> userPage = userRepository.findAll(UserSpecification.activeUsers(), pageable);
        Page<UserResponse> responsePage = userPage.map(UserResponse::from);

        return ResponseEntity.ok(PageResponse.from(responsePage));
    }

    @GetMapping("/locked")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get locked users",
            description = "Retrieve paginated list of locked users"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Locked users retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin only")
    })
    public ResponseEntity<PageResponse<UserResponse>> getLockedUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Fetching locked users - page: {}, size: {}", page, size);

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "accountLockedUntil"));

        Page<User> userPage = userRepository.findAll(UserSpecification.lockedUsers(), pageable);
        Page<UserResponse> responsePage = userPage.map(UserResponse::from);

        return ResponseEntity.ok(PageResponse.from(responsePage));
    }

    @GetMapping("/mfa-enabled")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get MFA-enabled users",
            description = "Retrieve paginated list of users with MFA enabled"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "MFA users retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin only")
    })
    public ResponseEntity<PageResponse<UserResponse>> getMfaEnabledUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        log.info("Fetching MFA-enabled users - page: {}, size: {}", page, size);

        size = Math.min(size, 100);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<User> userPage = userRepository.findAll(UserSpecification.mfaEnabledUsers(), pageable);
        Page<UserResponse> responsePage = userPage.map(UserResponse::from);

        return ResponseEntity.ok(PageResponse.from(responsePage));
    }

    @GetMapping("/statistics")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Get user statistics",
            description = "Retrieve overall user statistics"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Statistics retrieved"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "403", description = "Forbidden - Admin only")
    })
    public ResponseEntity<UserStatistics> getUserStatistics() {
        log.info("Fetching user statistics");

        long totalUsers = userRepository.count();
        long activeUsers = userRepository.count(UserSpecification.activeUsers());
        long lockedUsers = userRepository.count(UserSpecification.lockedUsers());
        long mfaEnabledUsers = userRepository.count(UserSpecification.mfaEnabledUsers());
        long adminUsers = userRepository.count(UserSpecification.hasRole(Role.ROLE_ADMIN));
        long regularUsers = userRepository.count(UserSpecification.hasRole(Role.ROLE_USER));

        UserStatistics stats = UserStatistics.builder()
                .totalUsers(totalUsers)
                .activeUsers(activeUsers)
                .lockedUsers(lockedUsers)
                .mfaEnabledUsers(mfaEnabledUsers)
                .adminUsers(adminUsers)
                .regularUsers(regularUsers)
                .mfaEnabledPercentage(totalUsers > 0 ? (double) mfaEnabledUsers / totalUsers * 100 : 0.0)
                .lockedPercentage(totalUsers > 0 ? (double) lockedUsers / totalUsers * 100 : 0.0)
                .build();

        return ResponseEntity.ok(stats);
    }

    /**
     * User statistics DTO.
     */
    @Schema(description = "User statistics")
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class UserStatistics {
        @Schema(description = "Total number of users")
        private long totalUsers;

        @Schema(description = "Number of active (non-locked) users")
        private long activeUsers;

        @Schema(description = "Number of locked users")
        private long lockedUsers;

        @Schema(description = "Number of users with MFA enabled")
        private long mfaEnabledUsers;

        @Schema(description = "Number of admin users")
        private long adminUsers;

        @Schema(description = "Number of regular users")
        private long regularUsers;

        @Schema(description = "Percentage of users with MFA enabled")
        private double mfaEnabledPercentage;

        @Schema(description = "Percentage of locked users")
        private double lockedPercentage;
    }
}
