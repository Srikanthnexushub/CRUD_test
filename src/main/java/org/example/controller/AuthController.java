package org.example.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.LoginRequest;
import org.example.dto.LoginResponse;
import org.example.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
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
