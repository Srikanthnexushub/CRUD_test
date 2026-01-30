package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.UserUpdateRequest;
import org.example.entity.User;
import org.example.service.UserService;
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

    @GetMapping
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
    public ResponseEntity<User> getUserById(
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
    public ResponseEntity<User> updateUser(
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
    public ResponseEntity<Void> deleteUser(
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
