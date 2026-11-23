package com.example.phoneauth.controller;

import com.example.phoneauth.dto.UserResponse;
import com.example.phoneauth.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    UserService userService;

    @Test
    void meReturnsCurrentUser() throws Exception {
        when(userService.getById(7L)).thenReturn(new UserResponse(7L, "13800138000", LocalDateTime.now()));

        mockMvc.perform(get("/api/users/me").requestAttr("userId", 7L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("13800138000"));
    }
}
