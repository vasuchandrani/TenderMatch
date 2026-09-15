package com.tendermatch.security;

import com.tendermatch.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private final String secret = "9a6747f3b6c4349479b009eb733a1cc00142b6045610816a7f8ca9b2520bb262";
    private final long expirationMs = 3600000; // 1 hr

    private User testUser;
    private CustomUserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "jwtSecret", secret);
        ReflectionTestUtils.setField(jwtService, "jwtExpirationMs", expirationMs);

        UUID userId = UUID.randomUUID();
        testUser = User.builder()
                .id(userId)
                .email("test@example.com")
                .name("Test User")
                .passwordHash("hashed")
                .role("USER")
                .build();
        userDetails = new CustomUserDetails(testUser);
    }

    @Test
    void shouldGenerateValidToken() {
        String token = jwtService.generateToken(userDetails, testUser.getId());

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertTrue(jwtService.validateToken(token));
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void shouldExtractUserIdAndUsernameFromToken() {
        String token = jwtService.generateToken(userDetails, testUser.getId());

        String extractedUsername = jwtService.extractUsername(token);
        UUID extractedUserId = jwtService.extractUserId(token);

        assertEquals("test@example.com", extractedUsername);
        assertEquals(testUser.getId(), extractedUserId);
    }

    @Test
    void shouldRejectExpiredToken() {
        // Create jwtService with negative expiration (already expired)
        JwtService expiredJwtService = new JwtService();
        ReflectionTestUtils.setField(expiredJwtService, "jwtSecret", secret);
        ReflectionTestUtils.setField(expiredJwtService, "jwtExpirationMs", -1000L);

        String expiredToken = expiredJwtService.generateToken(userDetails, testUser.getId());

        assertFalse(jwtService.validateToken(expiredToken));
        assertFalse(jwtService.isTokenValid(expiredToken, userDetails));
    }

    @Test
    void shouldRejectTamperedToken() {
        String token = jwtService.generateToken(userDetails, testUser.getId());
        // Tamper with the token signature
        String tamperedToken = token.substring(0, token.length() - 5) + "abcde";

        assertFalse(jwtService.validateToken(tamperedToken));
        assertFalse(jwtService.isTokenValid(tamperedToken, userDetails));
    }
}
