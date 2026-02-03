package org.example.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Collections;
import java.util.Date;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for JwtUtil
 * Tests JWT token generation, validation, and extraction
 */
@DisplayName("JwtUtil Tests")
class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String testSecret = "TestSecretKey_OnlyForAutomatedTesting_AtLeast256BitsLongForHS256Algorithm_DoNotUseInProduction";
    private final long testExpiration = 3600000L; // 1 hour

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", testSecret);
        ReflectionTestUtils.setField(jwtUtil, "expiration", testExpiration);
        jwtUtil.init(); // Initialize the secret key
    }

    @Test
    @DisplayName("Should generate valid JWT token")
    void shouldGenerateValidToken() {
        // Arrange
        Authentication authentication = createAuthentication("testuser");

        // Act
        String token = jwtUtil.generateToken(authentication);

        // Assert
        assertThat(token).isNotNull();
        assertThat(token).isNotEmpty();
        assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts: header.payload.signature
    }

    @Test
    @DisplayName("Should extract username from valid token")
    void shouldExtractUsernameFromToken() {
        // Arrange
        String username = "testuser";
        Authentication authentication = createAuthentication(username);
        String token = jwtUtil.generateToken(authentication);

        // Act
        String extractedUsername = jwtUtil.getUsernameFromToken(token);

        // Assert
        assertThat(extractedUsername).isEqualTo(username);
    }

    @Test
    @DisplayName("Should validate token successfully")
    void shouldValidateTokenSuccessfully() {
        // Arrange
        Authentication authentication = createAuthentication("testuser");
        String token = jwtUtil.generateToken(authentication);

        // Act
        boolean isValid = jwtUtil.validateToken(token);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Should reject expired token")
    void shouldRejectExpiredToken() {
        // Arrange - Create token with -1ms expiration (already expired)
        JwtUtil shortLivedJwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(shortLivedJwtUtil, "secret", testSecret);
        ReflectionTestUtils.setField(shortLivedJwtUtil, "expiration", -1L);
        shortLivedJwtUtil.init();

        Authentication authentication = createAuthentication("testuser");
        String token = shortLivedJwtUtil.generateToken(authentication);

        // Act & Assert
        assertThatThrownBy(() -> jwtUtil.validateToken(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Should reject token with invalid signature")
    void shouldRejectTokenWithInvalidSignature() {
        // Arrange
        Authentication authentication = createAuthentication("testuser");
        String token = jwtUtil.generateToken(authentication);

        // Tamper with the token by changing the signature
        String[] parts = token.split("\\.");
        String tamperedToken = parts[0] + "." + parts[1] + ".invalidsignature";

        // Act & Assert
        assertThatThrownBy(() -> jwtUtil.validateToken(tamperedToken))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    @DisplayName("Should reject malformed token")
    void shouldRejectMalformedToken() {
        // Arrange
        String malformedToken = "not.a.valid.jwt.token";

        // Act & Assert
        assertThatThrownBy(() -> jwtUtil.validateToken(malformedToken))
                .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    @DisplayName("Should reject null token")
    void shouldRejectNullToken() {
        // Act & Assert
        assertThatThrownBy(() -> jwtUtil.validateToken(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should reject empty token")
    void shouldRejectEmptyToken() {
        // Act & Assert
        assertThatThrownBy(() -> jwtUtil.validateToken(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should extract correct expiration date")
    void shouldExtractCorrectExpirationDate() {
        // Arrange
        Authentication authentication = createAuthentication("testuser");
        Instant beforeGeneration = Instant.now();
        String token = jwtUtil.generateToken(authentication);
        Instant afterGeneration = Instant.now();

        // Act
        Claims claims = jwtUtil.extractAllClaims(token);
        Date expiration = claims.getExpiration();

        // Assert
        Instant expectedExpiration = beforeGeneration.plusMillis(testExpiration);
        assertThat(expiration).isNotNull();
        assertThat(expiration.toInstant()).isAfter(expectedExpiration.minusSeconds(5)); // Allow 5 second tolerance
        assertThat(expiration.toInstant()).isBefore(afterGeneration.plusMillis(testExpiration).plusSeconds(5));
    }

    @Test
    @DisplayName("Should generate different tokens for different users")
    void shouldGenerateDifferentTokensForDifferentUsers() {
        // Arrange
        Authentication auth1 = createAuthentication("user1");
        Authentication auth2 = createAuthentication("user2");

        // Act
        String token1 = jwtUtil.generateToken(auth1);
        String token2 = jwtUtil.generateToken(auth2);

        // Assert
        assertThat(token1).isNotEqualTo(token2);
        assertThat(jwtUtil.getUsernameFromToken(token1)).isEqualTo("user1");
        assertThat(jwtUtil.getUsernameFromToken(token2)).isEqualTo("user2");
    }

    @Test
    @DisplayName("Should generate different tokens for same user at different times")
    void shouldGenerateDifferentTokensForSameUserAtDifferentTimes() throws InterruptedException {
        // Arrange
        Authentication authentication = createAuthentication("testuser");

        // Act
        String token1 = jwtUtil.generateToken(authentication);
        Thread.sleep(100); // Wait 100ms to ensure different issued-at time
        String token2 = jwtUtil.generateToken(authentication);

        // Assert
        assertThat(token1).isNotEqualTo(token2);
        assertThat(jwtUtil.getUsernameFromToken(token1)).isEqualTo("testuser");
        assertThat(jwtUtil.getUsernameFromToken(token2)).isEqualTo("testuser");
    }

    @Test
    @DisplayName("Should extract subject claim correctly")
    void shouldExtractSubjectClaimCorrectly() {
        // Arrange
        String username = "john_doe";
        Authentication authentication = createAuthentication(username);
        String token = jwtUtil.generateToken(authentication);

        // Act
        Claims claims = jwtUtil.extractAllClaims(token);
        String subject = claims.getSubject();

        // Assert
        assertThat(subject).isEqualTo(username);
    }

    @Test
    @DisplayName("Should have issued-at time before expiration time")
    void shouldHaveIssuedAtTimeBeforeExpirationTime() {
        // Arrange
        Authentication authentication = createAuthentication("testuser");
        String token = jwtUtil.generateToken(authentication);

        // Act
        Claims claims = jwtUtil.extractAllClaims(token);
        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();

        // Assert
        assertThat(issuedAt).isNotNull();
        assertThat(expiration).isNotNull();
        assertThat(issuedAt).isBefore(expiration);
        assertThat(expiration.getTime() - issuedAt.getTime()).isEqualTo(testExpiration);
    }

    @Test
    @DisplayName("Should handle usernames with special characters")
    void shouldHandleUsernamesWithSpecialCharacters() {
        // Arrange
        String specialUsername = "user_name-123.test@domain";
        Authentication authentication = createAuthentication(specialUsername);

        // Act
        String token = jwtUtil.generateToken(authentication);
        String extractedUsername = jwtUtil.getUsernameFromToken(token);

        // Assert
        assertThat(extractedUsername).isEqualTo(specialUsername);
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    @DisplayName("Should handle long usernames")
    void shouldHandleLongUsernames() {
        // Arrange
        String longUsername = "a".repeat(255); // Maximum common username length
        Authentication authentication = createAuthentication(longUsername);

        // Act
        String token = jwtUtil.generateToken(authentication);
        String extractedUsername = jwtUtil.getUsernameFromToken(token);

        // Assert
        assertThat(extractedUsername).isEqualTo(longUsername);
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    // Helper methods

    private Authentication createAuthentication(String username) {
        return new UsernamePasswordAuthenticationToken(
                username,
                null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }
}
