package com.example.phoneauth.service;

import com.example.phoneauth.dto.UserResponse;
import com.example.phoneauth.entity.User;
import com.example.phoneauth.exception.ApiException;
import com.example.phoneauth.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse getById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "用户不存在"));
        return UserResponse.from(user);
    }
}
