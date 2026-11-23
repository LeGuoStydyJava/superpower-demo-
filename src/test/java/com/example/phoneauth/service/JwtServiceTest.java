package com.example.phoneauth.service;

import com.example.phoneauth.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtServiceTest {

    private static final String SECRET = "a-super-secret-key-for-hs256-at-least-32-bytes-long!!";
    private final JwtService jwtService = new JwtService(SECRET, 60000L);

    @Test
    void generateThenParseReturnsSameUserId() {
        String token = jwtService.generateToken(42L);
        assertEquals(42L, jwtService.parseUserId(token));
    }

    @Test
    void parseTamperedTokenThrows401() {
        String token = jwtService.generateToken(42L);
        String tampered = token.substring(0, token.length() - 1)
                + (token.endsWith("A") ? "B" : "A");
        ApiException e = assertThrows(ApiException.class, () -> jwtService.parseUserId(tampered));
        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
    }

    @Test
    void parseExpiredTokenThrows401() {
        JwtService shortLived = new JwtService(SECRET, -1000L);
        String token = shortLived.generateToken(42L);
        ApiException e = assertThrows(ApiException.class, () -> shortLived.parseUserId(token));
        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
    }
}
