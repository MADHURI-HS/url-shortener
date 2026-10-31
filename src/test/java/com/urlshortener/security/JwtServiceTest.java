package com.urlshortener.security;

import com.urlshortener.model.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-test-secret-key-1234";

    private JwtService jwtService;
    private User user;

    private static JwtService serviceWith(String secret, long expirationMs) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "secret", secret);
        ReflectionTestUtils.setField(service, "expiration", expirationMs);
        return service;
    }

    @BeforeEach
    void setUp() {
        jwtService = serviceWith(SECRET, 900_000L);
        user = User.builder().email("jane@example.com").role("ROLE_USER").build();
    }

    @Test
    void generatedTokenCarriesTheUsersEmail() {
        String token = jwtService.generateToken(user);

        assertEquals("jane@example.com", jwtService.extractUsername(token));
    }

    @Test
    void freshTokenIsValidForItsOwner() {
        String token = jwtService.generateToken(user);

        assertTrue(jwtService.isTokenValid(token, user));
    }

    @Test
    void tokenIsNotValidForADifferentUser() {
        String token = jwtService.generateToken(user);
        User other = User.builder().email("other@example.com").role("ROLE_USER").build();

        assertFalse(jwtService.isTokenValid(token, other));
    }

    @Test
    void expiredTokenIsRejected() {
        String token = serviceWith(SECRET, -1_000L).generateToken(user);

        assertThrows(ExpiredJwtException.class, () -> jwtService.extractUsername(token));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String forged = serviceWith("another-secret-key-another-secret-key-99", 900_000L)
                .generateToken(user);

        assertThrows(JwtException.class, () -> jwtService.extractUsername(forged));
    }
}
