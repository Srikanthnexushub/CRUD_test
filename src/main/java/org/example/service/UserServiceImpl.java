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
        log.info("Attempting to authenticate user: {}", request.getUsername());

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            String token = jwtUtil.generateToken(authentication);

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new InvalidCredentialsException("Invalid username or password"));

            log.info("User authenticated successfully: {}", user.getUsername());

            return new LoginResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getRole());
        } catch (InvalidCredentialsException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Authentication failed for user: {}", request.getUsername());
            throw new InvalidCredentialsException("Invalid username or password");
        }
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

        return targetUser;
    }

    @Override
    @Transactional
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
}
