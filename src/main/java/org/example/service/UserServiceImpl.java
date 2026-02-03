package org.example.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.dto.LoginRequest;
import org.example.dto.LoginResponse;
import org.example.dto.UserUpdateRequest;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.exception.InvalidCredentialsException;
import org.example.exception.UnauthorizedException;
import org.example.exception.UserAlreadyExistsException;
import org.example.exception.UserNotFoundException;
import org.example.repository.UserRepository;
import org.example.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final AccountLockService accountLockService;

    @Override
    @Transactional
    public void registerUser(String username, String email, String password) {
        log.info("Attempting to register user: {}", username);

        if (userRepository.existsByUsername(username)) {
            log.warn("Registration failed: Username '{}' already exists", username);
            throw new UserAlreadyExistsException("Username already exists");
        }

        if (userRepository.existsByEmail(email)) {
            log.warn("Registration failed: Email '{}' already exists", email);
            throw new UserAlreadyExistsException("Email already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(Role.ROLE_USER);

        User savedUser = userRepository.save(user);
        log.info("User registered successfully: {}", savedUser.getUsername());
    }

    @Override
    public LoginResponse authenticateUser(LoginRequest request, jakarta.servlet.http.HttpServletRequest httpRequest) {
        String username = request.getUsername();
        String ipAddress = getClientIP(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");

        log.info("Attempting to authenticate user: {} from IP: {}", username, ipAddress);

        // Check if IP is blocked
        if (accountLockService.shouldBlockIp(ipAddress)) {
            log.warn("IP address {} is temporarily blocked due to too many failed attempts", ipAddress);
            accountLockService.recordFailedLogin(username, ipAddress, userAgent, "IP_BLOCKED");
            throw new InvalidCredentialsException("Too many failed login attempts. Please try again later.");
        }

        // Check if user exists and if account is locked
        User user = userRepository.findByUsername(username).orElse(null);

        if (user != null) {
            // Check and auto-unlock if lock expired
            accountLockService.checkAndAutoUnlock(user);

            // Check if account is still locked
            if (user.getIsAccountLocked()) {
                log.warn("Login attempt for locked account: {}", username);
                accountLockService.recordFailedLogin(username, ipAddress, userAgent, "ACCOUNT_LOCKED");
                throw new InvalidCredentialsException("Account is locked. Please try again later or contact support.");
            }
        }

        try {
            // Authenticate credentials
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, request.getPassword())
            );

            // Refresh user from database (may have been updated)
            user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

            // Record successful login
            accountLockService.recordSuccessfulLogin(username, ipAddress, userAgent);

            // Generate JWT token
            String token = jwtUtil.generateToken(authentication);

            log.info("User authenticated successfully: {}", user.getUsername());

            return new LoginResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getRole());

        } catch (InvalidCredentialsException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Authentication failed for user: {} - {}", username, e.getMessage());

            // Determine failure reason
            String failureReason = user == null ? "USER_NOT_FOUND" : "INVALID_PASSWORD";
            accountLockService.recordFailedLogin(username, ipAddress, userAgent, failureReason);

            throw new InvalidCredentialsException("Invalid username or password");
        }
    }

    /**
     * Extract client IP address from request, considering proxy headers.
     */
    private String getClientIP(jakarta.servlet.http.HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // Take first IP if there are multiple (proxy chain)
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "unknown";
    }

    @Override
    public List<User> getAllUsers(String currentUsername) {
        log.info("User '{}' attempting to retrieve all users", currentUsername);

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UserNotFoundException("Current user not found"));

        if (currentUser.getRole() != Role.ROLE_ADMIN) {
            log.warn("User '{}' is not authorized to view all users", currentUsername);
            throw new UnauthorizedException("You do not have permission to view all users");
        }

        return userRepository.findAll();
    }

    @Override
    @org.springframework.cache.annotation.Cacheable(value = "users", key = "#id")
    public User getUserById(Long id, String currentUsername) {
        log.info("User '{}' attempting to retrieve user with ID: {}", currentUsername, id);

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UserNotFoundException("Current user not found"));

        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));

        if (!isAdminOrOwner(id, currentUsername)) {
            log.warn("User '{}' is not authorized to view user with ID: {}", currentUsername, id);
            throw new UnauthorizedException("You do not have permission to view this user");
        }

        log.debug("Cache miss - User fetched from database: ID {}", id);
        return targetUser;
    }

    @Override
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = {"users", "userByUsername", "userByEmail"}, allEntries = true)
    public User updateUser(Long id, UserUpdateRequest request, String currentUsername) {
        log.info("User '{}' attempting to update user with ID: {}", currentUsername, id);

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UserNotFoundException("Current user not found"));

        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));

        if (!isAdminOrOwner(id, currentUsername)) {
            log.warn("User '{}' is not authorized to update user with ID: {}", currentUsername, id);
            throw new UnauthorizedException("You do not have permission to update this user");
        }

        if (request.getUsername() != null && !request.getUsername().equals(targetUser.getUsername())) {
            if (userRepository.existsByUsername(request.getUsername())) {
                throw new UserAlreadyExistsException("Username already exists");
            }
            targetUser.setUsername(request.getUsername());
        }

        if (request.getEmail() != null && !request.getEmail().equals(targetUser.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new UserAlreadyExistsException("Email already exists");
            }
            targetUser.setEmail(request.getEmail());
        }

        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            targetUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        // Only admins can change roles
        if (request.getRole() != null && currentUser.getRole() == Role.ROLE_ADMIN) {
            if (!targetUser.getRole().equals(request.getRole())) {
                log.info("Admin '{}' changing role of user '{}' from {} to {}",
                    currentUsername, targetUser.getUsername(), targetUser.getRole(), request.getRole());
                targetUser.setRole(request.getRole());
            }
        }

        User updatedUser = userRepository.save(targetUser);
        log.info("User with ID {} updated successfully", id);

        return updatedUser;
    }

    @Override
    @Transactional
    @org.springframework.cache.annotation.CacheEvict(value = {"users", "userByUsername", "userByEmail"}, allEntries = true)
    public void deleteUser(Long id, String currentUsername) {
        log.info("User '{}' attempting to delete user with ID: {}", currentUsername, id);

        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UserNotFoundException("Current user not found"));

        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + id));

        if (!isAdminOrOwner(id, currentUsername)) {
            log.warn("User '{}' is not authorized to delete user with ID: {}", currentUsername, id);
            throw new UnauthorizedException("You do not have permission to delete this user");
        }

        userRepository.deleteById(id);
        log.info("User with ID {} deleted successfully", id);
    }

    @Override
    public boolean isAdminOrOwner(Long userId, String currentUsername) {
        User currentUser = userRepository.findByUsername(currentUsername)
                .orElseThrow(() -> new UserNotFoundException("Current user not found"));

        if (currentUser.getRole() == Role.ROLE_ADMIN) {
            return true;
        }

        return currentUser.getId().equals(userId);
    }

    @Override
    @org.springframework.cache.annotation.Cacheable(value = "userByUsername", key = "#username")
    public java.util.Optional<User> findByUsername(String username) {
        log.debug("Cache miss - Fetching user from database: {}", username);
        return userRepository.findByUsername(username);
    }

    @Override
    public String generateAuthToken(User user) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getUsername(),
                null,
                List.of(() -> user.getRole().name())
        );
        return jwtUtil.generateToken(authentication);
    }
}
