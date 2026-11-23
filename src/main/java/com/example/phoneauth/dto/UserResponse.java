package com.example.phoneauth.dto;

import com.example.phoneauth.entity.User;

import java.time.LocalDateTime;

public record UserResponse(Long id, String phone, LocalDateTime createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getPhone(), user.getCreatedAt());
    }
}
