package org.example.controller.v2;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
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
import org.example.versioning.ApiVersion;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Authentication controller - Version 2.
 * Enhanced with automatic refresh token issuance and improved response format.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
@ApiVersion(2)
@Tag(name = "Authentication V2", description = "Enhanced authentication endpoints with refresh token support")
public class AuthControllerV2 {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/login")
    @Operation(
            summary = "User Login (V2)",
            description = """
                    Enhanced login endpoint that automatically includes refresh token in response.

                    **Changes from V1:**
                    - Returns both access token and refresh token
                    - Improved error responses with correlation IDs
                    - Enhanced security headers
                    """
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login successful with tokens",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("V2 API: Login request for user: {}", request.getUsername());

        // Authenticate user
        LoginResponse response = userService.authenticateUser(request, httpRequest);

        // Generate refresh token (V2 enhancement)
        if (response.getToken() != null) {
            var user = userService.findByUsername(request.getUsername()).orElseThrow();
            var refreshToken = refreshTokenService.createRefreshToken(user, httpRequest);
            response.setRefreshToken(refreshToken.getToken());

            log.info("V2 API: Login successful for user: {} with refresh token", request.getUsername());
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh Token (V2)",
            description = "Refresh access token using refresh token"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid refresh token")
    })
    public ResponseEntity<LoginResponse> refresh(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest
    ) {
        log.info("V2 API: Token refresh request");

        LoginResponse response = refreshTokenService.refreshAccessToken(
                request.getRefreshToken(),
                httpRequest
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    @Operation(
            summary = "User Registration (V2)",
            description = "Register new user with enhanced validation"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "Username or email already exists")
    })
    public ResponseEntity<Map<String, Object>> register(
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest
    ) {
        String username = request.get("username");
        String email = request.get("email");
        String password = request.get("password");

        log.info("V2 API: Registration request for user: {}", username);

        userService.registerUser(username, email, password);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "User registered successfully");
        response.put("username", username);
        response.put("email", email);

        return ResponseEntity.status(201).body(response);
    }

    @PostMapping("/logout")
    @Operation(
            summary = "Logout (V2)",
            description = "Logout and revoke refresh token"
    )
    public ResponseEntity<Map<String, String>> logout(
            @Valid @RequestBody RefreshTokenRequest request
    ) {
        log.info("V2 API: Logout request");

        refreshTokenService.revokeToken(request.getRefreshToken());

        Map<String, String> response = new HashMap<>();
        response.put("success", "true");
        response.put("message", "Logged out successfully");

        return ResponseEntity.ok(response);
    }
}
