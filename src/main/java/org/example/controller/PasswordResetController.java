package org.example.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.dto.PasswordResetInitiateRequest;
import org.example.dto.PasswordResetRequest;
import org.example.service.PasswordResetService;
import org.example.util.RequestUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Password Reset Controller
 * Handles password reset flow
 */
@RestController
@RequestMapping("/api/v1/password-reset")
@RequiredArgsConstructor
@Tag(name = "Password Reset", description = "Password reset endpoints")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @PostMapping("/initiate")
    @Operation(summary = "Initiate password reset", description = "Send password reset email to user")
    public ResponseEntity<Map<String, String>> initiatePasswordReset(
            @Valid @RequestBody PasswordResetInitiateRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = RequestUtils.getClientIpAddress(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        passwordResetService.initiatePasswordReset(request, ipAddress, userAgent);

        return ResponseEntity.ok(Map.of(
            "message", "If the email exists, a password reset link has been sent",
            "status", "success"
        ));
    }

    @GetMapping("/validate")
    @Operation(summary = "Validate reset token", description = "Check if reset token is valid")
    public ResponseEntity<Map<String, Object>> validateToken(@RequestParam String token) {
        boolean isValid = passwordResetService.validateResetToken(token);

        return ResponseEntity.ok(Map.of(
            "valid", isValid,
            "message", isValid ? "Token is valid" : "Token is invalid or expired"
        ));
    }

    @PostMapping("/reset")
    @Operation(summary = "Reset password", description = "Reset password using valid token")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody PasswordResetRequest request,
            HttpServletRequest httpRequest) {

        String ipAddress = RequestUtils.getClientIpAddress(httpRequest);
        passwordResetService.resetPassword(request, ipAddress);

        return ResponseEntity.ok(Map.of(
            "message", "Password has been successfully reset",
            "status", "success"
        ));
    }
}
