package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.LoginRequest;
import org.example.dto.LoginResponse;
import org.example.dto.RefreshTokenRequest;
import org.example.exception.ErrorResponse;
import org.example.service.RefreshTokenService;
import org.example.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Public endpoints for user authentication and registration. No JWT token required.")
public class AuthController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    @Operation(
            summary = "User Login",
            description = """
                    Authenticate a user with username and password. Returns a JWT token valid for 1 hour.

                    **Authentication Flow:**
                    1. Submit username and password
                    2. Receive JWT token in response
                    3. Include token in Authorization header for protected endpoints: `Bearer {token}`

                    **Default Admin Credentials (Development):**
                    - Username: `admin`
                    - Password: `admin123`
                    """)
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Login successful - JWT token returned",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponse.class),
                            examples = @ExampleObject(
                                    name = "Successful Login",
                                    value = """
                                            {
                                                "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
                                                "type": "Bearer",
                                                "id": 1,
                                                "username": "admin",
                                                "email": "admin@crudtest.com",
                                                "role": "ROLE_ADMIN",
                                                "mfaEnabled": false,
                                                "accountLocked": false
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request - Missing or invalid fields",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Missing Username",
                                    value = """
                                            {
                                                "timestamp": "2026-02-03T10:15:30",
                                                "status": 400,
                                                "error": "Bad Request",
                                                "message": "Validation failed",
                                                "details": ["username: must not be blank"]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication failed - Invalid credentials",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Invalid Credentials",
                                    value = """
                                            {
                                                "timestamp": "2026-02-03T10:15:30",
                                                "status": 401,
                                                "error": "Unauthorized",
                                                "message": "Invalid username or password",
                                                "details": []
                                            }
                                            """
                            )
                    )
            )
    })
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request,
                                               HttpServletRequest httpRequest) {
        String clientIp = getClientIp(httpRequest);
        log.info("=== LOGIN REQUEST START === User: '{}', IP: {}, UserAgent: {}",
            request.getUsername(), clientIp, httpRequest.getHeader("User-Agent"));

        try {
            LoginResponse response = userService.authenticateUser(request, httpRequest);
            log.info("=== LOGIN SUCCESS === User: '{}', ID: {}, Role: {}, IP: {}",
                response.getUsername(), response.getId(), response.getRole(), clientIp);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("=== LOGIN FAILED === User: '{}', IP: {}, Error: {}",
                request.getUsername(), clientIp, e.getMessage());
            throw e;
        }
    }

    @PostMapping("/register")
    @Operation(
            summary = "User Registration",
            description = """
                    Create a new user account. Username and email must be unique.

                    **Password Requirements:**
                    - Minimum 8 characters
                    - At least one uppercase letter
                    - At least one lowercase letter
                    - At least one digit

                    **Username Requirements:**
                    - 3-50 characters
                    - Alphanumeric and underscores only

                    **Default Role:** All new users are assigned `ROLE_USER` role.
                    """)
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Registration successful",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success Response",
                                    value = """
                                            {
                                                "message": "User registered successfully"
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid request - Validation failed",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ErrorResponse.class),
                            examples = @ExampleObject(
                                    name = "Weak Password",
                                    value = """
                                            {
                                                "timestamp": "2026-02-03T10:15:30",
                                                "status": 400,
                                                "error": "Bad Request",
                                                "message": "Validation failed",
                                                "details": [
                                                    "password: must contain at least one uppercase letter",
                                                    "password: must contain at least one digit"
                                                ]
                                            }
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Conflict - Username or email already exists",
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
    public ResponseEntity<Map<String, String>> register(@RequestBody Map<String, String> request,
                                                        HttpServletRequest httpRequest) {
        String username = request.get("username");
        String email = request.get("email");
        String password = request.get("password");
        String clientIp = getClientIp(httpRequest);

        log.info("=== REGISTRATION REQUEST START === User: '{}', Email: '{}', IP: {}",
            username, email, clientIp);

        try {
            userService.registerUser(username, email, password);
            log.info("=== REGISTRATION SUCCESS === User: '{}', Email: '{}', IP: {}",
                username, email, clientIp);

            Map<String, String> response = new HashMap<>();
            response.put("message", "User registered successfully");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("=== REGISTRATION FAILED === User: '{}', Email: '{}', IP: {}, Error: {}",
                username, email, clientIp, e.getMessage());
            throw e;
        }
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh Access Token",
            description = """
                    Refresh an expired access token using a valid refresh token.

                    **Token Rotation:** When you refresh, you receive:
                    - A new access token (valid for 1 hour)
                    - A new refresh token (valid for 7 days)
                    - The old refresh token is revoked

                    **Use Cases:**
                    - Access token expired but refresh token is still valid
                    - Extend user session without re-authentication
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or expired refresh token",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<LoginResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {

        log.info("=== TOKEN REFRESH REQUEST === IP: {}", getClientIp(httpRequest));

        try {
            LoginResponse response = refreshTokenService.refreshAccessToken(
                    request.getRefreshToken(),
                    httpRequest
            );

            log.info("=== TOKEN REFRESH SUCCESS === User: {}, IP: {}",
                    response.getUsername(), getClientIp(httpRequest));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("=== TOKEN REFRESH FAILED === IP: {}, Error: {}",
                    getClientIp(httpRequest), e.getMessage());
            throw e;
        }
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Logout",
            description = """
                    Logout and revoke refresh token. Access token will remain valid until expiration.

                    **Best Practice:** Client should:
                    1. Call this endpoint with refresh token
                    2. Delete access token and refresh token from storage
                    3. Redirect to login page
                    """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logout successful"),
            @ApiResponse(responseCode = "400", description = "Invalid refresh token")
    })
    public ResponseEntity<Map<String, String>> logout(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest) {

        log.info("=== LOGOUT REQUEST === IP: {}", getClientIp(httpRequest));

        try {
            refreshTokenService.revokeToken(request.getRefreshToken());

            Map<String, String> response = new HashMap<>();
            response.put("message", "Logged out successfully");

            log.info("=== LOGOUT SUCCESS === IP: {}", getClientIp(httpRequest));

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("=== LOGOUT FAILED === IP: {}, Error: {}",
                    getClientIp(httpRequest), e.getMessage());
            throw e;
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
