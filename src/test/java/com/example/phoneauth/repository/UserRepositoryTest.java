package com.example.phoneauth.repository;

import com.example.phoneauth.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    UserRepository userRepository;

    @Test
    void findByPhoneAndExistsByPhone() {
        User u = new User();
        u.setPhone("13800138000");
        u.setPasswordHash("hashed");
        userRepository.save(u);

        assertTrue(userRepository.existsByPhone("13800138000"));
        assertFalse(userRepository.existsByPhone("13900139000"));
        assertEquals("13800138000", userRepository.findByPhone("13800138000").orElseThrow().getPhone());
        assertNotNull(u.getCreatedAt());
    }
}
