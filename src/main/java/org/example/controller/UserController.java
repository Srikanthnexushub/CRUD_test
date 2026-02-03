package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.UserUpdateRequest;
import org.example.entity.User;
import org.example.exception.ErrorResponse;
import org.example.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Management", description = "Protected endpoints for managing user accounts. Requires JWT authentication.")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(
            summary = "Get All Users",
            description = """
                    Retrieve a list of all registered users.

                    **Authorization:** ADMIN role required

                    **Note:** Regular users (ROLE_USER) cannot access this endpoint.
                    """)
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Users retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = User.class),
                            examples = @ExampleObject(
                                    name = "User List",
                                    value = """
                                            [
                                                {
                                                    "id": 1,
                                                    "username": "admin",
                                                    "email": "admin@crudtest.com",
                                                    "role": "ROLE_ADMIN",
                                                    "mfaEnabled": false,
                                                    "accountLocked": false,
                                                    "createdAt": "2026-02-01T10:00:00",
                                                    "updatedAt": "2026-02-01T10:00:00"
                                                },
                                                {
                                                    "id": 2,
                                                    "username": "testuser",
                                                    "email": "test@test.com",
                                                    "role": "ROLE_USER",
                                                    "mfaEnabled": false,
                                                    "accountLocked": false,
                                                    "createdAt": "2026-02-02T15:30:00",
                                                    "updatedAt": "2026-02-02T15:30:00"
                                                }
                                            ]
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User does not have ADMIN role",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Not Admin",
                                    value = """
                                            {
                                                "timestamp": "2026-02-03T10:15:30",
                                                "status": 403,
                                                "error": "Forbidden",
                                                "message": "You do not have permission to view all users",
                                                "details": []
                                            }
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<List<User>> getAllUsers(Authentication authentication) {
        String currentUsername = authentication.getName();
        log.info("=== GET ALL USERS REQUEST === RequestedBy: '{}'", currentUsername);

        try {
            List<User> users = userService.getAllUsers(currentUsername);
            log.info("=== GET ALL USERS SUCCESS === RequestedBy: '{}', ReturnedCount: {}",
                currentUsername, users.size());
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("=== GET ALL USERS FAILED === RequestedBy: '{}', Error: {}",
                currentUsername, e.getMessage(), e);
            throw e;
        }
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get User By ID",
            description = """
                    Retrieve a specific user by their ID.

                    **Authorization:**
                    - ADMIN can view any user
                    - Regular user can only view their own profile
                    """)
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User retrieved successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = User.class),
                            examples = @ExampleObject(
                                    name = "User Details",
                                    value = """
                                            {
                                                "id": 1,
                                                "username": "admin",
                                                "email": "admin@crudtest.com",
                                                "role": "ROLE_ADMIN",
                                                "mfaEnabled": false,
                                                "accountLocked": false,
                                                "accountLockedUntil": null,
                                                "lockReason": null,
                                                "createdAt": "2026-02-01T10:00:00",
                                                "updatedAt": "2026-02-01T10:00:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User cannot view other users' profiles",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Unauthorized Access",
                                    value = """
                                            {
                                                "timestamp": "2026-02-03T10:15:30",
                                                "status": 403,
                                                "error": "Forbidden",
                                                "message": "You do not have permission to view this user",
                                                "details": []
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found - User with specified ID does not exist",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "User Not Found",
                                    value = """
                                            {
                                                "timestamp": "2026-02-03T10:15:30",
                                                "status": 404,
                                                "error": "Not Found",
                                                "message": "User not found with ID: 999",
                                                "details": []
                                            }
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<User> getUserById(
            @Parameter(description = "User ID", required = true, example = "1")
            @PathVariable Long id,
            Authentication authentication) {
        String currentUsername = authentication.getName();
        log.info("=== GET USER REQUEST === UserID: {}, RequestedBy: '{}'", id, currentUsername);

        try {
            User user = userService.getUserById(id, currentUsername);
            log.info("=== GET USER SUCCESS === UserID: {}, Username: '{}', RequestedBy: '{}'",
                id, user.getUsername(), currentUsername);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("=== GET USER FAILED === UserID: {}, RequestedBy: '{}', Error: {}",
                id, currentUsername, e.getMessage(), e);
            throw e;
        }
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update User",
            description = """
                    Update user information. All fields are optional (partial update supported).

                    **Authorization:**
                    - ADMIN can update any user
                    - Regular user can only update their own profile

                    **Role Changes:**
                    - Only ADMIN can change user roles
                    - Regular users cannot modify the role field

                    **Password Updates:**
                    - New password will be hashed with BCrypt (strength 12)
                    - Password complexity requirements apply
                    """)
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "User updated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = User.class),
                            examples = @ExampleObject(
                                    name = "Updated User",
                                    value = """
                                            {
                                                "id": 1,
                                                "username": "newusername",
                                                "email": "newemail@test.com",
                                                "role": "ROLE_ADMIN",
                                                "mfaEnabled": false,
                                                "accountLocked": false,
                                                "createdAt": "2026-02-01T10:00:00",
                                                "updatedAt": "2026-02-03T14:30:00"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Bad Request - Validation failed",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Invalid Email",
                                    value = """
                                            {
                                                "timestamp": "2026-02-03T10:15:30",
                                                "status": 400,
                                                "error": "Bad Request",
                                                "message": "Validation failed",
                                                "details": ["email: must be a valid email address"]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User cannot update other users",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found - User does not exist",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict - Username or email already taken",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Duplicate Username",
                                    value = """
                                            {
                                                "timestamp": "2026-02-03T10:15:30",
                                                "status": 409,
                                                "error": "Conflict",
                                                "message": "Username already exists",
                                                "details": []
                                            }
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<User> updateUser(
            @Parameter(description = "User ID", required = true, example = "1")
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request,
            Authentication authentication) {
        String currentUsername = authentication.getName();
        log.info("=== UPDATE USER REQUEST === UserID: {}, RequestedBy: '{}', UpdateFields: [username={}, email={}, role={}, passwordChanged={}]",
            id, currentUsername, request.getUsername(), request.getEmail(),
            request.getRole(), request.getPassword() != null && !request.getPassword().isEmpty());

        try {
            User user = userService.updateUser(id, request, currentUsername);
            log.info("=== UPDATE USER SUCCESS === UserID: {}, Username: '{}', RequestedBy: '{}'",
                id, user.getUsername(), currentUsername);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("=== UPDATE USER FAILED === UserID: {}, RequestedBy: '{}', Error: {}",
                id, currentUsername, e.getMessage(), e);
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete User",
            description = """
                    Permanently delete a user account.

                    **Authorization:**
                    - ADMIN can delete any user
                    - Regular user can only delete their own account

                    **Warning:** This action is irreversible. All user data will be permanently deleted.
                    """)
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204",
                    description = "User deleted successfully - No content returned",
                    content = @Content()
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized - Invalid or missing JWT token",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "Forbidden - User cannot delete other users",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Unauthorized Delete",
                                    value = """
                                            {
                                                "timestamp": "2026-02-03T10:15:30",
                                                "status": 403,
                                                "error": "Forbidden",
                                                "message": "You do not have permission to delete this user",
                                                "details": []
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Not Found - User does not exist",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "User Not Found",
                                    value = """
                                            {
                                                "timestamp": "2026-02-03T10:15:30",
                                                "status": 404,
                                                "error": "Not Found",
                                                "message": "User not found with ID: 999",
                                                "details": []
                                            }
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "User ID to delete", required = true, example = "1")
            @PathVariable Long id,
            Authentication authentication) {
        String currentUsername = authentication.getName();
        log.info("=== DELETE USER REQUEST === UserID: {}, RequestedBy: '{}'", id, currentUsername);

        try {
            userService.deleteUser(id, currentUsername);
            log.info("=== DELETE USER SUCCESS === UserID: {}, RequestedBy: '{}'", id, currentUsername);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("=== DELETE USER FAILED === UserID: {}, RequestedBy: '{}', Error: {}",
                id, currentUsername, e.getMessage(), e);
            throw e;
        }
    }
}
