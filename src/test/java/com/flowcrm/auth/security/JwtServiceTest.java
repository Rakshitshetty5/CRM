package com.flowcrm.auth.security;

import com.flowcrm.auth.entity.User;
import com.flowcrm.common.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private UserPrincipal testUserPrincipal;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", "test-secret-key-1234567890-abcdefghijklmnopqrstuvwxyz");
        ReflectionTestUtils.setField(jwtService, "accessTokenExpirationMs", 3600000L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpirationMs", 86400000L);

        User user = User.builder()
                .id(UUID.randomUUID())
                .firstName("Alice")
                .lastName("Smith")
                .email("alice@example.com")
                .password("hashed_password")
                .role(Role.ADMIN)
                .active(true)
                .build();

        testUserPrincipal = new UserPrincipal(user);
    }

    @Test
    @DisplayName("Generate and validate JWT access token")
    void generateAndValidateAccessToken() {
        String token = jwtService.generateAccessToken(testUserPrincipal);
        assertNotNull(token);
        assertTrue(jwtService.validateToken(token));
        assertEquals("alice@example.com", jwtService.getUsernameFromToken(token));
    }

    @Test
    @DisplayName("Validate token returns false for invalid token string")
    void validateInvalidToken() {
        assertFalse(jwtService.validateToken("invalid.token.string"));
    }

    @Test
    @DisplayName("SHA-256 token hashing produces consistent hex string")
    void hashToken_Consistency() {
        String rawToken = "my-secret-refresh-token-123";
        String hash1 = jwtService.hashToken(rawToken);
        String hash2 = jwtService.hashToken(rawToken);

        assertNotNull(hash1);
        assertEquals(64, hash1.length()); // SHA-256 hex string length is 64
        assertEquals(hash1, hash2);
        assertNotEquals(rawToken, hash1);
    }
}
