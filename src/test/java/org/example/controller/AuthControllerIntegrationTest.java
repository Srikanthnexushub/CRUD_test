package org.example.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.dto.LoginRequest;
import org.example.entity.Role;
import org.example.entity.User;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController
 * Tests full HTTP request/response cycle with in-memory database
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Auth Controller Integration Tests")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("POST /api/auth/register - Should register new user successfully")
    void shouldRegisterNewUserSuccessfully() throws Exception {
        // Arrange
        String requestBody = """
                {
                    "username": "newuser",
                    "email": "newuser@test.com",
                    "password": "Test@1234"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(content().string("User registered successfully"));

        // Verify user was created in database
        assert userRepository.findByUsername("newuser").isPresent();
        assert userRepository.findByUsername("newuser").get().getRole() == Role.ROLE_USER;
    }

    @Test
    @DisplayName("POST /api/auth/register - Should return 400 for missing username")
    void shouldReturn400ForMissingUsername() throws Exception {
        // Arrange
        String requestBody = """
                {
                    "email": "test@test.com",
                    "password": "Test@1234"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("POST /api/auth/register - Should return 400 for invalid email format")
    void shouldReturn400ForInvalidEmailFormat() throws Exception {
        // Arrange
        String requestBody = """
                {
                    "username": "testuser",
                    "email": "invalid-email",
                    "password": "Test@1234"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/auth/register - Should return 400 for weak password")
    void shouldReturn400ForWeakPassword() throws Exception {
        // Arrange
        String requestBody = """
                {
                    "username": "testuser",
                    "email": "test@test.com",
                    "password": "weak"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/auth/register - Should return 409 for duplicate username")
    void shouldReturn409ForDuplicateUsername() throws Exception {
        // Arrange - Create existing user
        User existingUser = new User();
        existingUser.setUsername("existinguser");
        existingUser.setEmail("existing@test.com");
        existingUser.setPasswordHash(passwordEncoder.encode("Test@1234"));
        existingUser.setRole(Role.ROLE_USER);
        userRepository.save(existingUser);

        String requestBody = """
                {
                    "username": "existinguser",
                    "email": "different@test.com",
                    "password": "Test@1234"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Username already exists"));
    }

    @Test
    @DisplayName("POST /api/auth/register - Should return 409 for duplicate email")
    void shouldReturn409ForDuplicateEmail() throws Exception {
        // Arrange - Create existing user
        User existingUser = new User();
        existingUser.setUsername("existinguser");
        existingUser.setEmail("existing@test.com");
        existingUser.setPasswordHash(passwordEncoder.encode("Test@1234"));
        existingUser.setRole(Role.ROLE_USER);
        userRepository.save(existingUser);

        String requestBody = """
                {
                    "username": "differentuser",
                    "email": "existing@test.com",
                    "password": "Test@1234"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Should login successfully with valid credentials")
    void shouldLoginSuccessfullyWithValidCredentials() throws Exception {
        // Arrange - Create user
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@test.com");
        user.setPasswordHash(passwordEncoder.encode("Test@1234"));
        user.setRole(Role.ROLE_USER);
        userRepository.save(user);

        String requestBody = """
                {
                    "username": "testuser",
                    "password": "Test@1234"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@test.com"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.id").exists());
    }

    @Test
    @DisplayName("POST /api/auth/login - Should return 401 for invalid username")
    void shouldReturn401ForInvalidUsername() throws Exception {
        // Arrange
        String requestBody = """
                {
                    "username": "nonexistent",
                    "password": "Test@1234"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Should return 401 for invalid password")
    void shouldReturn401ForInvalidPassword() throws Exception {
        // Arrange - Create user
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@test.com");
        user.setPasswordHash(passwordEncoder.encode("Test@1234"));
        user.setRole(Role.ROLE_USER);
        userRepository.save(user);

        String requestBody = """
                {
                    "username": "testuser",
                    "password": "WrongPassword"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password"));
    }

    @Test
    @DisplayName("POST /api/auth/login - Should return 400 for missing username")
    void shouldReturn400ForMissingUsernameInLogin() throws Exception {
        // Arrange
        String requestBody = """
                {
                    "password": "Test@1234"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/auth/login - Should return 400 for missing password")
    void shouldReturn400ForMissingPassword() throws Exception {
        // Arrange
        String requestBody = """
                {
                    "username": "testuser"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/auth/login - Should generate different tokens for same user")
    void shouldGenerateDifferentTokensForSameUser() throws Exception {
        // Arrange - Create user
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@test.com");
        user.setPasswordHash(passwordEncoder.encode("Test@1234"));
        user.setRole(Role.ROLE_USER);
        userRepository.save(user);

        String requestBody = """
                {
                    "username": "testuser",
                    "password": "Test@1234"
                }
                """;

        // Act - Login twice
        String response1 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Thread.sleep(100); // Small delay to ensure different issued-at time

        String response2 = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Assert - Tokens should be different
        assert !response1.equals(response2);
    }

    @Test
    @DisplayName("POST /api/auth/login - Should login admin user and return ROLE_ADMIN")
    void shouldLoginAdminUser() throws Exception {
        // Arrange - Create admin user
        User admin = new User();
        admin.setUsername("admin");
        admin.setEmail("admin@test.com");
        admin.setPasswordHash(passwordEncoder.encode("Admin@1234"));
        admin.setRole(Role.ROLE_ADMIN);
        userRepository.save(admin);

        String requestBody = """
                {
                    "username": "admin",
                    "password": "Admin@1234"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ROLE_ADMIN"))
                .andExpect(jsonPath("$.username").value("admin"));
    }

    @Test
    @DisplayName("POST /api/auth/register - Should trim whitespace from username and email")
    void shouldTrimWhitespaceFromUsernameAndEmail() throws Exception {
        // Arrange
        String requestBody = """
                {
                    "username": "  testuser  ",
                    "email": "  test@test.com  ",
                    "password": "Test@1234"
                }
                """;

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk());

        // Verify trimmed values
        User user = userRepository.findByUsername("testuser").orElseThrow();
        assert user.getUsername().equals("testuser");
        assert user.getEmail().equals("test@test.com");
    }
}
