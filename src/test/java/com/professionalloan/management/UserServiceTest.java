package com.professionalloan.management;

import com.professionalloan.management.exception.DuplicateLoanApplicationException;
import com.professionalloan.management.exception.UserNotFoundException;
import com.professionalloan.management.model.User;
import com.professionalloan.management.repository.UserRepository;
import com.professionalloan.management.service.UserService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    @InjectMocks
    private UserService service;

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void registerUser_duplicateEmail_throws() {
        User user = new User();
        user.setEmail("test@test.com");
        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        assertThrows(DuplicateLoanApplicationException.class, () -> service.registerUser(user));
    }

    @Test
    void registerUser_success() {
        User user = new User();
        user.setEmail("unique@test.com");
        when(userRepository.findByEmail("unique@test.com")).thenReturn(Optional.empty());
        assertTrue(service.registerUser(user));
    }

    @Test
    void findByEmailAndPassword_adminLogin() {
        User admin = service.findByEmailAndPassword("admin@gmail.com", "admin");
        assertEquals("ADMIN", admin.getRole());
    }

    @Test
    void findByEmail_userNotFound() {
        when(userRepository.findByEmail("x@y.com")).thenReturn(Optional.empty());
        assertThrows(UserNotFoundException.class, () -> service.findByEmail("x@y.com"));
    }
}