package org.example.service;

import jakarta.servlet.http.HttpServletRequest;
import org.example.TestDataBuilder;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserServiceImpl
 * Tests all business logic without database dependencies
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Tests")
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private HttpServletRequest httpRequest;

    @InjectMocks
    private UserServiceImpl userService;

    @Nested
    @DisplayName("User Registration Tests")
    class RegistrationTests {

        @Test
        @DisplayName("Should register user successfully")
        void shouldRegisterUserSuccessfully() {
            // Arrange
            String username = "newuser";
            String email = "newuser@test.com";
            String password = "Test@1234";
            String encodedPassword = "encoded_password";

            when(userRepository.existsByUsername(username)).thenReturn(false);
            when(userRepository.existsByEmail(email)).thenReturn(false);
            when(passwordEncoder.encode(password)).thenReturn(encodedPassword);
            when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // Act
            userService.registerUser(username, email, password);

            // Assert
            verify(userRepository).existsByUsername(username);
            verify(userRepository).existsByEmail(email);
            verify(passwordEncoder).encode(password);
            verify(userRepository).save(argThat(user ->
                    user.getUsername().equals(username) &&
                    user.getEmail().equals(email) &&
                    user.getPasswordHash().equals(encodedPassword) &&
                    user.getRole() == Role.ROLE_USER
            ));
        }

        @Test
        @DisplayName("Should throw exception when username already exists")
        void shouldThrowExceptionWhenUsernameExists() {
            // Arrange
            String username = "existinguser";
            when(userRepository.existsByUsername(username)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> userService.registerUser(username, "test@test.com", "password"))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("Username already exists");

            verify(userRepository).existsByUsername(username);
            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw exception when email already exists")
        void shouldThrowExceptionWhenEmailExists() {
            // Arrange
            String email = "existing@test.com";
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(email)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> userService.registerUser("newuser", email, "password"))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("Email already exists");

            verify(userRepository).existsByEmail(email);
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("User Authentication Tests")
    class AuthenticationTests {

        @Test
        @DisplayName("Should authenticate user successfully")
        void shouldAuthenticateUserSuccessfully() {
            // Arrange
            String username = "testuser";
            String password = "Test@1234";
            String token = "jwt.token.here";
            User user = TestDataBuilder.user().username(username).id(1L).build();

            LoginRequest request = new LoginRequest();
            request.setUsername(username);
            request.setPassword(password);

            Authentication authentication = mock(Authentication.class);

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(authentication);
            when(jwtUtil.generateToken(authentication)).thenReturn(token);
            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

            // Act
            LoginResponse response = userService.authenticateUser(request, httpRequest);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo(token);
            assertThat(response.getId()).isEqualTo(user.getId());
            assertThat(response.getUsername()).isEqualTo(username);
            assertThat(response.getEmail()).isEqualTo(user.getEmail());
            assertThat(response.getRole()).isEqualTo(user.getRole());

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(jwtUtil).generateToken(authentication);
        }

        @Test
        @DisplayName("Should throw exception on invalid credentials")
        void shouldThrowExceptionOnInvalidCredentials() {
            // Arrange
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("wrongpassword");

            when(authenticationManager.authenticate(any()))
                    .thenThrow(new BadCredentialsException("Invalid credentials"));

            // Act & Assert
            assertThatThrownBy(() -> userService.authenticateUser(request, httpRequest))
                    .isInstanceOf(InvalidCredentialsException.class)
                    .hasMessageContaining("Invalid username or password");
        }

        @Test
        @DisplayName("Should throw exception when user not found after authentication")
        void shouldThrowExceptionWhenUserNotFoundAfterAuth() {
            // Arrange
            String username = "testuser";
            LoginRequest request = new LoginRequest();
            request.setUsername(username);
            request.setPassword("Test@1234");

            Authentication authentication = mock(Authentication.class);

            when(authenticationManager.authenticate(any())).thenReturn(authentication);
            when(jwtUtil.generateToken(authentication)).thenReturn("token");
            when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.authenticateUser(request, httpRequest))
                    .isInstanceOf(InvalidCredentialsException.class);
        }
    }

    @Nested
    @DisplayName("Get All Users Tests")
    class GetAllUsersTests {

        @Test
        @DisplayName("Should return all users for admin")
        void shouldReturnAllUsersForAdmin() {
            // Arrange
            String adminUsername = "admin";
            User admin = TestDataBuilder.adminUser().username(adminUsername).build();
            List<User> allUsers = Arrays.asList(
                    TestDataBuilder.user().id(1L).build(),
                    TestDataBuilder.user().id(2L).build(),
                    TestDataBuilder.user().id(3L).build()
            );

            when(userRepository.findByUsername(adminUsername)).thenReturn(Optional.of(admin));
            when(userRepository.findAll()).thenReturn(allUsers);

            // Act
            List<User> result = userService.getAllUsers(adminUsername);

            // Assert
            assertThat(result).hasSize(3);
            assertThat(result).isEqualTo(allUsers);
            verify(userRepository).findAll();
        }

        @Test
        @DisplayName("Should throw UnauthorizedException for non-admin")
        void shouldThrowUnauthorizedExceptionForNonAdmin() {
            // Arrange
            String username = "regularuser";
            User user = TestDataBuilder.user().username(username).build();

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

            // Act & Assert
            assertThatThrownBy(() -> userService.getAllUsers(username))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("You do not have permission to view all users");

            verify(userRepository, never()).findAll();
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when current user not found")
        void shouldThrowUserNotFoundExceptionWhenCurrentUserNotFound() {
            // Arrange
            String username = "nonexistent";
            when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.getAllUsers(username))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("Get User By ID Tests")
    class GetUserByIdTests {

        @Test
        @DisplayName("Should return user when requested by admin")
        void shouldReturnUserWhenRequestedByAdmin() {
            // Arrange
            String adminUsername = "admin";
            Long targetUserId = 5L;
            User admin = TestDataBuilder.adminUser().username(adminUsername).id(1L).build();
            User targetUser = TestDataBuilder.user().id(targetUserId).build();

            when(userRepository.findByUsername(adminUsername)).thenReturn(Optional.of(admin));
            when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

            // Act
            User result = userService.getUserById(targetUserId, adminUsername);

            // Assert
            assertThat(result).isEqualTo(targetUser);
        }

        @Test
        @DisplayName("Should return user when requested by owner")
        void shouldReturnUserWhenRequestedByOwner() {
            // Arrange
            String username = "testuser";
            Long userId = 5L;
            User user = TestDataBuilder.user().username(username).id(userId).build();

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            // Act
            User result = userService.getUserById(userId, username);

            // Assert
            assertThat(result).isEqualTo(user);
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when user is not admin or owner")
        void shouldThrowUnauthorizedExceptionWhenNotAdminOrOwner() {
            // Arrange
            String username = "user1";
            Long targetUserId = 99L;
            User currentUser = TestDataBuilder.user().username(username).id(1L).build();
            User targetUser = TestDataBuilder.user().id(targetUserId).build();

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(currentUser));
            when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

            // Act & Assert
            assertThatThrownBy(() -> userService.getUserById(targetUserId, username))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessageContaining("You do not have permission to view this user");
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when target user not found")
        void shouldThrowUserNotFoundExceptionWhenTargetUserNotFound() {
            // Arrange
            String adminUsername = "admin";
            Long targetUserId = 999L;
            User admin = TestDataBuilder.adminUser().username(adminUsername).build();

            when(userRepository.findByUsername(adminUsername)).thenReturn(Optional.of(admin));
            when(userRepository.findById(targetUserId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.getUserById(targetUserId, adminUsername))
                    .isInstanceOf(UserNotFoundException.class)
                    .hasMessageContaining("User not found with ID: " + targetUserId);
        }
    }

    @Nested
    @DisplayName("Update User Tests")
    class UpdateUserTests {

        @Test
        @DisplayName("Should update username when requested by owner")
        void shouldUpdateUsernameWhenRequestedByOwner() {
            // Arrange
            String currentUsername = "oldusername";
            String newUsername = "newusername";
            Long userId = 1L;
            User user = TestDataBuilder.user().username(currentUsername).id(userId).build();
            UserUpdateRequest request = TestDataBuilder.userUpdateRequest().username(newUsername).build();

            when(userRepository.findByUsername(currentUsername)).thenReturn(Optional.of(user));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.existsByUsername(newUsername)).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            User result = userService.updateUser(userId, request, currentUsername);

            // Assert
            assertThat(result.getUsername()).isEqualTo(newUsername);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should update email when requested by owner")
        void shouldUpdateEmailWhenRequestedByOwner() {
            // Arrange
            String username = "testuser";
            String newEmail = "newemail@test.com";
            Long userId = 1L;
            User user = TestDataBuilder.user().username(username).id(userId).build();
            UserUpdateRequest request = TestDataBuilder.userUpdateRequest().email(newEmail).build();

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.existsByEmail(newEmail)).thenReturn(false);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            User result = userService.updateUser(userId, request, username);

            // Assert
            assertThat(result.getEmail()).isEqualTo(newEmail);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should update password when requested by owner")
        void shouldUpdatePasswordWhenRequestedByOwner() {
            // Arrange
            String username = "testuser";
            String newPassword = "NewPassword@123";
            String encodedPassword = "encoded_new_password";
            Long userId = 1L;
            User user = TestDataBuilder.user().username(username).id(userId).build();
            UserUpdateRequest request = TestDataBuilder.userUpdateRequest().password(newPassword).build();

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(passwordEncoder.encode(newPassword)).thenReturn(encodedPassword);
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            User result = userService.updateUser(userId, request, username);

            // Assert
            assertThat(result.getPasswordHash()).isEqualTo(encodedPassword);
            verify(passwordEncoder).encode(newPassword);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should allow admin to change user role")
        void shouldAllowAdminToChangeUserRole() {
            // Arrange
            String adminUsername = "admin";
            Long targetUserId = 5L;
            User admin = TestDataBuilder.adminUser().username(adminUsername).id(1L).build();
            User targetUser = TestDataBuilder.user().id(targetUserId).build();
            UserUpdateRequest request = TestDataBuilder.userUpdateRequest().role(Role.ROLE_ADMIN).build();

            when(userRepository.findByUsername(adminUsername)).thenReturn(Optional.of(admin));
            when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            User result = userService.updateUser(targetUserId, request, adminUsername);

            // Assert
            assertThat(result.getRole()).isEqualTo(Role.ROLE_ADMIN);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("Should not allow regular user to change role")
        void shouldNotAllowRegularUserToChangeRole() {
            // Arrange
            String username = "testuser";
            Long userId = 1L;
            User user = TestDataBuilder.user().username(username).id(userId).build();
            UserUpdateRequest request = TestDataBuilder.userUpdateRequest().role(Role.ROLE_ADMIN).build();

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            // Act
            User result = userService.updateUser(userId, request, username);

            // Assert
            assertThat(result.getRole()).isEqualTo(Role.ROLE_USER); // Role unchanged
        }

        @Test
        @DisplayName("Should throw UserAlreadyExistsException when new username exists")
        void shouldThrowExceptionWhenNewUsernameExists() {
            // Arrange
            String username = "testuser";
            String newUsername = "existinguser";
            Long userId = 1L;
            User user = TestDataBuilder.user().username(username).id(userId).build();
            UserUpdateRequest request = TestDataBuilder.userUpdateRequest().username(newUsername).build();

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));
            when(userRepository.existsByUsername(newUsername)).thenReturn(true);

            // Act & Assert
            assertThatThrownBy(() -> userService.updateUser(userId, request, username))
                    .isInstanceOf(UserAlreadyExistsException.class)
                    .hasMessageContaining("Username already exists");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when user is not admin or owner")
        void shouldThrowUnauthorizedExceptionWhenNotAdminOrOwner() {
            // Arrange
            String username = "user1";
            Long targetUserId = 99L;
            User currentUser = TestDataBuilder.user().username(username).id(1L).build();
            User targetUser = TestDataBuilder.user().id(targetUserId).build();
            UserUpdateRequest request = TestDataBuilder.userUpdateRequest().username("newname").build();

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(currentUser));
            when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

            // Act & Assert
            assertThatThrownBy(() -> userService.updateUser(targetUserId, request, username))
                    .isInstanceOf(UnauthorizedException.class);

            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("Delete User Tests")
    class DeleteUserTests {

        @Test
        @DisplayName("Should delete user when requested by admin")
        void shouldDeleteUserWhenRequestedByAdmin() {
            // Arrange
            String adminUsername = "admin";
            Long targetUserId = 5L;
            User admin = TestDataBuilder.adminUser().username(adminUsername).id(1L).build();
            User targetUser = TestDataBuilder.user().id(targetUserId).build();

            when(userRepository.findByUsername(adminUsername)).thenReturn(Optional.of(admin));
            when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

            // Act
            userService.deleteUser(targetUserId, adminUsername);

            // Assert
            verify(userRepository).deleteById(targetUserId);
        }

        @Test
        @DisplayName("Should delete user when requested by owner")
        void shouldDeleteUserWhenRequestedByOwner() {
            // Arrange
            String username = "testuser";
            Long userId = 1L;
            User user = TestDataBuilder.user().username(username).id(userId).build();

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
            when(userRepository.findById(userId)).thenReturn(Optional.of(user));

            // Act
            userService.deleteUser(userId, username);

            // Assert
            verify(userRepository).deleteById(userId);
        }

        @Test
        @DisplayName("Should throw UnauthorizedException when user is not admin or owner")
        void shouldThrowUnauthorizedExceptionWhenNotAdminOrOwner() {
            // Arrange
            String username = "user1";
            Long targetUserId = 99L;
            User currentUser = TestDataBuilder.user().username(username).id(1L).build();
            User targetUser = TestDataBuilder.user().id(targetUserId).build();

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(currentUser));
            when(userRepository.findById(targetUserId)).thenReturn(Optional.of(targetUser));

            // Act & Assert
            assertThatThrownBy(() -> userService.deleteUser(targetUserId, username))
                    .isInstanceOf(UnauthorizedException.class);

            verify(userRepository, never()).deleteById(any());
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when target user not found")
        void shouldThrowUserNotFoundExceptionWhenTargetUserNotFound() {
            // Arrange
            String adminUsername = "admin";
            Long targetUserId = 999L;
            User admin = TestDataBuilder.adminUser().username(adminUsername).build();

            when(userRepository.findByUsername(adminUsername)).thenReturn(Optional.of(admin));
            when(userRepository.findById(targetUserId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.deleteUser(targetUserId, adminUsername))
                    .isInstanceOf(UserNotFoundException.class);

            verify(userRepository, never()).deleteById(any());
        }
    }

    @Nested
    @DisplayName("Authorization Helper Tests")
    class AuthorizationHelperTests {

        @Test
        @DisplayName("Should return true for admin user")
        void shouldReturnTrueForAdmin() {
            // Arrange
            String adminUsername = "admin";
            Long targetUserId = 5L;
            User admin = TestDataBuilder.adminUser().username(adminUsername).id(1L).build();

            when(userRepository.findByUsername(adminUsername)).thenReturn(Optional.of(admin));

            // Act
            boolean result = userService.isAdminOrOwner(targetUserId, adminUsername);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return true for owner")
        void shouldReturnTrueForOwner() {
            // Arrange
            String username = "testuser";
            Long userId = 5L;
            User user = TestDataBuilder.user().username(username).id(userId).build();

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

            // Act
            boolean result = userService.isAdminOrOwner(userId, username);

            // Assert
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should return false for different user")
        void shouldReturnFalseForDifferentUser() {
            // Arrange
            String username = "user1";
            Long targetUserId = 99L;
            User user = TestDataBuilder.user().username(username).id(1L).build();

            when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));

            // Act
            boolean result = userService.isAdminOrOwner(targetUserId, username);

            // Assert
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("Should throw UserNotFoundException when current user not found")
        void shouldThrowUserNotFoundExceptionWhenCurrentUserNotFound() {
            // Arrange
            String username = "nonexistent";
            when(userRepository.findByUsername(username)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> userService.isAdminOrOwner(1L, username))
                    .isInstanceOf(UserNotFoundException.class);
        }
    }
}
