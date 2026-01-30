package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.User;
import org.example.repository.UserRepository;
import org.example.service.MFAService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mfa")
@RequiredArgsConstructor
@Slf4j
public class MFAController {

    private final MFAService mfaService;
    private final UserRepository userRepository;

    @PostMapping("/setup")
    public ResponseEntity<?> setupMFA(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            Map<String, Object> setup = mfaService.setupMFA(user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", setup);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to setup MFA", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to setup MFA: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/verify-setup")
    public ResponseEntity<?> verifyAndEnableMFA(@RequestBody Map<String, String> request, Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String totpCode = request.get("code");

        boolean verified = mfaService.verifyAndEnableMFA(user.getId(), totpCode);

        if (verified) {
            // Generate and return backup codes
            List<String> backupCodes = mfaService.regenerateBackupCodes(user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "MFA enabled successfully");
            response.put("backupCodes", backupCodes);

            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Invalid verification code");

            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/disable")
    public ResponseEntity<?> disableMFA(@RequestBody Map<String, String> request, Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String password = request.get("password");

        // TODO: Verify password before disabling MFA
        // For now, just disable

        try {
            mfaService.disableMFA(user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "MFA disabled successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to disable MFA", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to disable MFA: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    @PostMapping("/regenerate-backup-codes")
    public ResponseEntity<?> regenerateBackupCodes(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        try {
            List<String> backupCodes = mfaService.regenerateBackupCodes(user.getId());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("backupCodes", backupCodes);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to regenerate backup codes", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to regenerate backup codes: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getMFAStatus(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> status = mfaService.getMFAStatus(user.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("status", status);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/trusted-devices")
    public ResponseEntity<?> getTrustedDevices(Principal principal) {
        User user = userRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        List<Map<String, Object>> devices = mfaService.getTrustedDevices(user.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("devices", devices);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/trusted-devices/{deviceId}")
    public ResponseEntity<?> removeTrustedDevice(@PathVariable Long deviceId, Principal principal) {
        try {
            mfaService.removeTrustedDevice(deviceId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Device removed successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to remove trusted device", e);

            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Failed to remove device: " + e.getMessage());

            return ResponseEntity.status(500).body(response);
        }
    }
}
