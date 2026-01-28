package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.UserRegistrationRequest;
import org.example.dto.UserRegistrationResponse;
import org.example.dto.UserResponse;
import org.example.dto.UserUpdateRequest;
import org.example.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<UserRegistrationResponse> registerUser(
            @Valid @RequestBody UserRegistrationRequest request) {
        log.info("Received registration request for username: {}", request.getUsername());
        UserRegistrationResponse response = userService.registerUser(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> getAllUsers(Authentication authentication) {
        log.info("Received request to get all users");
        String currentUsername = authentication.getName();
        List<UserResponse> users = userService.getAllUsers(currentUsername);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long id,
            Authentication authentication) {
        log.info("Received request to get user with ID: {}", id);
        String currentUsername = authentication.getName();
        UserResponse user = userService.getUserById(id, currentUsername);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request,
            Authentication authentication) {
        log.info("Received request to update user with ID: {}", id);
        String currentUsername = authentication.getName();
        UserResponse user = userService.updateUser(id, request, currentUsername);
        return ResponseEntity.ok(user);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            Authentication authentication) {
        log.info("Received request to delete user with ID: {}", id);
        String currentUsername = authentication.getName();
        userService.deleteUser(id, currentUsername);
        return ResponseEntity.noContent().build();
    }
}
