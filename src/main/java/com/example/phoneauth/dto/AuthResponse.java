package com.example.phoneauth.dto;

public record AuthResponse(String token, UserResponse user) {
}
