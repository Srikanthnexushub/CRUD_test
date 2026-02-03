package org.example;

import org.example.dto.LoginRequest;
import org.example.dto.UserUpdateRequest;
import org.example.entity.Role;
import org.example.entity.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;

/**
 * Test Data Builder for creating test entities and DTOs
 * Provides fluent API for building test data with sensible defaults
 */
public class TestDataBuilder {

    private static final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder(12);

    /**
     * User Builder
     */
    public static class UserBuilder {
        private Long id = 1L;
        private String username = "testuser";
        private String email = "testuser@test.com";
        private String password = "Test@1234";
        private String passwordHash = passwordEncoder.encode("Test@1234");
        private Role role = Role.ROLE_USER;
        private boolean mfaEnabled = false;
        private boolean isAccountLocked = false;
        private LocalDateTime accountLockedUntil = null;
        private String lockReason = null;
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime updatedAt = LocalDateTime.now();

        public UserBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UserBuilder username(String username) {
            this.username = username;
            return this;
        }

        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder password(String password) {
            this.password = password;
            this.passwordHash = passwordEncoder.encode(password);
            return this;
        }

        public UserBuilder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }

        public UserBuilder role(Role role) {
            this.role = role;
            return this;
        }

        public UserBuilder admin() {
            this.role = Role.ROLE_ADMIN;
            return this;
        }

        public UserBuilder mfaEnabled(boolean mfaEnabled) {
            this.mfaEnabled = mfaEnabled;
            return this;
        }

        public UserBuilder accountLocked(boolean locked) {
            this.isAccountLocked = locked;
            if (locked) {
                this.accountLockedUntil = LocalDateTime.now().plusMinutes(30);
                this.lockReason = "Test lock reason";
            }
            return this;
        }

        public UserBuilder accountLockedUntil(LocalDateTime accountLockedUntil) {
            this.accountLockedUntil = accountLockedUntil;
            return this;
        }

        public UserBuilder lockReason(String lockReason) {
            this.lockReason = lockReason;
            return this;
        }

        public UserBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public UserBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public User build() {
            User user = new User();
            user.setId(id);
            user.setUsername(username);
            user.setEmail(email);
            user.setPasswordHash(passwordHash);
            user.setRole(role);
            user.setMfaEnabled(mfaEnabled);
            user.setAccountLocked(isAccountLocked);
            user.setAccountLockedUntil(accountLockedUntil);
            user.setLockReason(lockReason);
            user.setCreatedAt(createdAt);
            user.setUpdatedAt(updatedAt);
            return user;
        }
    }

    /**
     * LoginRequest Builder
     */
    public static class LoginRequestBuilder {
        private String username = "testuser";
        private String password = "Test@1234";

        public LoginRequestBuilder username(String username) {
            this.username = username;
            return this;
        }

        public LoginRequestBuilder password(String password) {
            this.password = password;
            return this;
        }

        public LoginRequest build() {
            LoginRequest request = new LoginRequest();
            request.setUsername(username);
            request.setPassword(password);
            return request;
        }
    }

    /**
     * UserUpdateRequest Builder
     */
    public static class UserUpdateRequestBuilder {
        private String username;
        private String email;
        private String password;
        private Role role;

        public UserUpdateRequestBuilder username(String username) {
            this.username = username;
            return this;
        }

        public UserUpdateRequestBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserUpdateRequestBuilder password(String password) {
            this.password = password;
            return this;
        }

        public UserUpdateRequestBuilder role(Role role) {
            this.role = role;
            return this;
        }

        public UserUpdateRequest build() {
            UserUpdateRequest request = new UserUpdateRequest();
            request.setUsername(username);
            request.setEmail(email);
            request.setPassword(password);
            request.setRole(role);
            return request;
        }
    }

    // Factory methods
    public static UserBuilder user() {
        return new UserBuilder();
    }

    public static UserBuilder adminUser() {
        return new UserBuilder()
                .username("admin")
                .email("admin@test.com")
                .role(Role.ROLE_ADMIN);
    }

    public static LoginRequestBuilder loginRequest() {
        return new LoginRequestBuilder();
    }

    public static UserUpdateRequestBuilder userUpdateRequest() {
        return new UserUpdateRequestBuilder();
    }

    // Convenience methods for common scenarios
    public static User createRegularUser() {
        return user().build();
    }

    public static User createAdminUser() {
        return adminUser().build();
    }

    public static User createUserWithId(Long id) {
        return user().id(id).build();
    }

    public static User createUserWithUsername(String username) {
        return user().username(username).email(username + "@test.com").build();
    }

    public static User createLockedUser() {
        return user().accountLocked(true).build();
    }
}
