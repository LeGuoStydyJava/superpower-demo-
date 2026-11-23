package com.example.phoneauth.service;

import com.example.phoneauth.dto.RegisterRequest;
import com.example.phoneauth.dto.UserResponse;
import com.example.phoneauth.entity.User;
import com.example.phoneauth.exception.ApiException;
import com.example.phoneauth.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public UserResponse register(RegisterRequest request) {
        if (userRepository.existsByPhone(request.phone())) {
            throw new ApiException(HttpStatus.CONFLICT, "该手机号已注册");
        }
        User user = new User();
        user.setPhone(request.phone());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        User saved = userRepository.save(user);
        return UserResponse.from(saved);
    }
}
