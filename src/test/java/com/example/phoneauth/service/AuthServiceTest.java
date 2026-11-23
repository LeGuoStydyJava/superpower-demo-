package com.example.phoneauth.service;

import com.example.phoneauth.dto.RegisterRequest;
import com.example.phoneauth.dto.UserResponse;
import com.example.phoneauth.entity.User;
import com.example.phoneauth.exception.ApiException;
import com.example.phoneauth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtService jwtService;

    @InjectMocks
    AuthService authService;

    @Test
    void registerHashesPasswordAndReturnsUserResponse() {
        RegisterRequest req = new RegisterRequest("13800138000", "secret123");
        when(userRepository.existsByPhone("13800138000")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });

        UserResponse resp = authService.register(req);

        assertEquals("13800138000", resp.phone());
        assertNotNull(resp.id());
        verify(passwordEncoder).encode("secret123");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertEquals("hashed", captor.getValue().getPasswordHash());
    }

    @Test
    void registerDuplicatePhoneThrows409() {
        RegisterRequest req = new RegisterRequest("13800138000", "secret123");
        when(userRepository.existsByPhone("13800138000")).thenReturn(true);

        ApiException e = assertThrows(ApiException.class, () -> authService.register(req));
        assertEquals(HttpStatus.CONFLICT, e.getStatus());
    }
}
