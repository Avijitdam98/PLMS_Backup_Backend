package com.professionalloan.management.service;

import com.professionalloan.management.model.User;
import com.professionalloan.management.repository.UserRepository;
import com.professionalloan.management.exception.UserNotFoundException;
import com.professionalloan.management.exception.DuplicateLoanApplicationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    // Registration method (throws Duplicate exception if email exists)
    public boolean registerUser(User user) {
        Optional<User> existingUser = userRepository.findByEmail(user.getEmail());
        if (existingUser.isPresent()) {
            throw new DuplicateLoanApplicationException("Email already registered!");
        }
        userRepository.save(user);
        return true;
    }

    // Strict admin-only login logic
    public User findByEmailAndPassword(String email, String password) {
        // Admin login (hardcoded)
        if ("admin@gmail.com".equals(email) && "admin".equals(password)) {
            User admin = new User();
            admin.setId(0L); // Set a fixed ID for admin
            admin.setEmail("admin@gmail.com");
            admin.setPassword("admin");
            admin.setName("Admin");
            admin.setRole("ADMIN");
            return admin;
        }
        // Prevent any normal user from logging in with admin email
        if ("admin@gmail.com".equals(email)) {
            throw new UserNotFoundException("Invalid admin credentials!");
        }
        // Normal user login (from DB)
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getPassword().equals(password)) {
                // Only set role if not already set
                if (user.getRole() == null) {
                    user.setRole("USER");
                }
                return user;
            } else {
                throw new UserNotFoundException("Invalid password!");
            }
        }
        throw new UserNotFoundException("User not found with email: " + email);
    }

    // For OTP flow: throws exception if user not found
    public User findByEmail(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        return userOpt.orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }

    // Find user by ID (needed for profile update)
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    // Save user (no exception handling needed here)
    public void save(User user) {
        userRepository.save(user);
    }
}