package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.*;
import org.example.entity.User;
import org.example.service.MFAService;
import org.example.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * REST Controller for Multi-Factor Authentication operations.
 * Provides endpoints for MFA setup, verification, and device management.
 */
@RestController
@RequestMapping("/api/mfa")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Multi-Factor Authentication", description = "MFA setup and management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class MFAController {

    private final MFAService mfaService;
    private final UserService userService;

    /**
     * Generate MFA setup with QR code and backup codes.
     */
    @GetMapping("/setup")
    @Operation(summary = "Generate MFA setup",
               description = "Generates TOTP secret, QR code, and backup codes. MFA is not enabled until verified with /enable endpoint.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "MFA setup generated successfully",
                    content = @Content(schema = @Schema(implementation = MFASetupResponse.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "400", description = "MFA is already enabled")
    })
    public ResponseEntity<MFASetupResponse> setupMFA() {
        User user = getCurrentUser();

        if (user.getMfaEnabled()) {
            throw new IllegalStateException("MFA is already enabled. Disable it first to regenerate setup.");
        }

        MFASetupResponse response = mfaService.generateMFASetup(user);
        log.info("MFA setup generated for user: {}", user.getUsername());

        return ResponseEntity.ok(response);
    }

    /**
     * Enable MFA by verifying TOTP code.
     */
    @PostMapping("/enable")
    @Operation(summary = "Enable MFA",
               description = "Verifies TOTP code and enables MFA for the user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "MFA enabled successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid TOTP code or MFA setup not found"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Map<String, String>> enableMFA(@Valid @RequestBody MFAEnableRequest request) {
        User user = getCurrentUser();

        mfaService.enableMFA(user, request.getTotpCode());

        Map<String, String> response = new HashMap<>();
        response.put("message", "MFA has been successfully enabled for your account.");
        response.put("username", user.getUsername());

        log.info("MFA enabled for user: {}", user.getUsername());

        return ResponseEntity.ok(response);
    }

    /**
     * Disable MFA.
     */
    @PostMapping("/disable")
    @Operation(summary = "Disable MFA",
               description = "Disables MFA and removes all MFA settings and trusted devices")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "MFA disabled successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Map<String, String>> disableMFA() {
        User user = getCurrentUser();

        mfaService.disableMFA(user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "MFA has been disabled for your account.");
        response.put("username", user.getUsername());

        log.info("MFA disabled for user: {}", user.getUsername());

        return ResponseEntity.ok(response);
    }

    /**
     * Verify MFA code during login (called by AuthController).
     */
    @PostMapping("/verify")
    @Operation(summary = "Verify MFA code",
               description = "Verifies TOTP code or backup code during login. Returns JWT token if successful.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "MFA verified successfully",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid MFA code")
    })
    public ResponseEntity<LoginResponse> verifyMFA(
            @Valid @RequestBody MFAVerifyRequest request,
            HttpServletRequest httpRequest) {

        // Find user by username
        User user = userService.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Verify code
        boolean isValid = mfaService.verifyCode(user, request.getCode());

        if (!isValid) {
            log.warn("Invalid MFA code for user: {}", user.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Trust device if requested
        if (Boolean.TRUE.equals(request.getTrustDevice())) {
            String ipAddress = getClientIP(httpRequest);
            String userAgent = httpRequest.getHeader("User-Agent");
            String deviceIdentifier = mfaService.generateDeviceIdentifier(ipAddress, userAgent);
            String deviceName = request.getDeviceName() != null ? request.getDeviceName() : "Unknown Device";

            mfaService.trustDevice(user, deviceIdentifier, deviceName, ipAddress, userAgent);
            log.info("Device trusted for user: {} - Device: {}", user.getUsername(), deviceName);
        }

        // Generate JWT token
        String token = userService.generateAuthToken(user);

        LoginResponse response = new LoginResponse();
        response.setToken(token);
        response.setUsername(user.getUsername());
        response.setRole(user.getRole().name());

        log.info("MFA verification successful for user: {}", user.getUsername());

        return ResponseEntity.ok(response);
    }

    /**
     * Regenerate backup codes.
     */
    @PostMapping("/backup-codes/regenerate")
    @Operation(summary = "Regenerate backup codes",
               description = "Generates new backup codes and invalidates old ones")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Backup codes regenerated successfully"),
            @ApiResponse(responseCode = "400", description = "MFA is not enabled"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Map<String, Object>> regenerateBackupCodes() {
        User user = getCurrentUser();

        if (!user.getMfaEnabled()) {
            throw new IllegalStateException("MFA is not enabled for this user.");
        }

        String[] backupCodes = mfaService.regenerateBackupCodes(user);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Backup codes have been regenerated. Save them in a secure location.");
        response.put("backupCodes", backupCodes);

        log.info("Backup codes regenerated for user: {}", user.getUsername());

        return ResponseEntity.ok(response);
    }

    /**
     * Get all trusted devices.
     */
    @GetMapping("/trusted-devices")
    @Operation(summary = "Get trusted devices",
               description = "Lists all trusted devices for the current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Trusted devices retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<List<TrustedDeviceResponse>> getTrustedDevices() {
        User user = getCurrentUser();

        List<TrustedDeviceResponse> devices = mfaService.getTrustedDevices(user);

        log.info("Retrieved {} trusted devices for user: {}", devices.size(), user.getUsername());

        return ResponseEntity.ok(devices);
    }

    /**
     * Revoke a specific trusted device.
     */
    @DeleteMapping("/trusted-devices/{deviceId}")
    @Operation(summary = "Revoke trusted device",
               description = "Revokes a specific trusted device")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Device revoked successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "Device not found")
    })
    public ResponseEntity<Void> revokeTrustedDevice(@PathVariable Long deviceId) {
        User user = getCurrentUser();

        mfaService.revokeTrustedDevice(user, deviceId);

        log.info("Trusted device {} revoked for user: {}", deviceId, user.getUsername());

        return ResponseEntity.noContent().build();
    }

    /**
     * Revoke all trusted devices.
     */
    @DeleteMapping("/trusted-devices")
    @Operation(summary = "Revoke all trusted devices",
               description = "Revokes all trusted devices for the current user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "All devices revoked successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    public ResponseEntity<Map<String, String>> revokeAllTrustedDevices() {
        User user = getCurrentUser();

        mfaService.revokeAllTrustedDevices(user);

        Map<String, String> response = new HashMap<>();
        response.put("message", "All trusted devices have been revoked.");

        log.info("All trusted devices revoked for user: {}", user.getUsername());

        return ResponseEntity.ok(response);
    }

    // ========================================
    // Helper Methods
    // ========================================

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        return userService.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("User not found: " + username));
    }

    private String getClientIP(HttpServletRequest request) {
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
