package org.example.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.entity.UserSession;
import org.example.service.SessionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for session management
 */
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@Slf4j
public class SessionController {

    private final SessionService sessionService;

    /**
     * Get active sessions for current user
     */
    @GetMapping("/active")
    public ResponseEntity<List<UserSession>> getActiveSessions(@RequestParam Long userId) {
        List<UserSession> sessions = sessionService.getActiveSessions(userId);
        return ResponseEntity.ok(sessions);
    }

    /**
     * Get all sessions for a user (with pagination)
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<UserSession>> getUserSessions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserSession> sessions = sessionService.getUserSessions(userId, pageable);
        return ResponseEntity.ok(sessions);
    }

    /**
     * Terminate a specific session
     */
    @DeleteMapping("/{sessionToken}")
    public ResponseEntity<Void> terminateSession(@PathVariable String sessionToken) {
        sessionService.terminateSession(sessionToken, "USER_REQUESTED");
        return ResponseEntity.ok().build();
    }

    /**
     * Terminate all sessions for a user (admin only)
     */
    @DeleteMapping("/user/{userId}/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> terminateAllUserSessions(@PathVariable Long userId) {
        sessionService.terminateAllUserSessions(userId, "ADMIN_TERMINATED");
        return ResponseEntity.ok().build();
    }

    /**
     * Get active session count for a user
     */
    @GetMapping("/user/{userId}/count")
    public ResponseEntity<Long> getActiveSessionCount(@PathVariable Long userId) {
        long count = sessionService.getActiveSessionCount(userId);
        return ResponseEntity.ok(count);
    }
}
