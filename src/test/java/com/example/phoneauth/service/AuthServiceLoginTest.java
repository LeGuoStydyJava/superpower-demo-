package com.example.phoneauth.service;

import com.example.phoneauth.dto.AuthResponse;
import com.example.phoneauth.dto.LoginRequest;
import com.example.phoneauth.entity.User;
import com.example.phoneauth.exception.ApiException;
import com.example.phoneauth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceLoginTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtService jwtService;

    @InjectMocks
    AuthService authService;

    private User user() {
        User u = new User();
        u.setId(7L);
        u.setPhone("13800138000");
        u.setPasswordHash("hashed");
        return u;
    }

    @Test
    void loginSuccessReturnsTokenAndUser() {
        when(userRepository.findByPhone("13800138000")).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches("secret123", "hashed")).thenReturn(true);
        when(jwtService.generateToken(7L)).thenReturn("jwt-token");

        AuthResponse resp = authService.login(new LoginRequest("13800138000", "secret123"));

        assertEquals("jwt-token", resp.token());
        assertEquals("13800138000", resp.user().phone());
    }

    @Test
    void loginWrongPasswordThrows401() {
        when(userRepository.findByPhone("13800138000")).thenReturn(Optional.of(user()));
        when(passwordEncoder.matches("bad", "hashed")).thenReturn(false);

        ApiException e = assertThrows(ApiException.class,
                () -> authService.login(new LoginRequest("13800138000", "bad")));
        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
    }

    @Test
    void loginUnknownPhoneThrows401() {
        when(userRepository.findByPhone("13800138000")).thenReturn(Optional.empty());

        ApiException e = assertThrows(ApiException.class,
                () -> authService.login(new LoginRequest("13800138000", "secret123")));
        assertEquals(HttpStatus.UNAUTHORIZED, e.getStatus());
    }
}
