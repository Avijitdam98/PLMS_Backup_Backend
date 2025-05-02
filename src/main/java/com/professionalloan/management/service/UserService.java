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

    @Autowired
    private EmailService emailService;

    //  Register user (fails if email is already registered)
    public boolean registerUser(User user) {
        Optional<User> existingUser = userRepository.findByEmail(user.getEmail());
        if (existingUser.isPresent()) {
            throw new DuplicateLoanApplicationException("Email already registered!");
        }

        // Assign role if not set
        if (user.getRole() == null) {
            user.setRole("USER");
        }

        userRepository.save(user);

        // Send registration success email
        emailService.sendRegistrationSuccessEmail(user.getEmail(), user.getName());

        return true;
    }

    //  Admin-only login with hardcoded credentials
    public User findByEmailAndPassword(String email, String password) {
        // Admin login
        if ("admin@gmail.com".equals(email) && "admin".equals(password)) {
            User admin = new User();
            admin.setId(0L); 
            admin.setEmail("admin@gmail.com");
            admin.setPassword("admin");
            admin.setName("Admin");
            admin.setRole("ADMIN");
            return admin;
        }

        // Prevent login for anyone else using admin email
        if ("admin@gmail.com".equals(email)) {
            throw new UserNotFoundException("Invalid admin credentials!");
        }

        //  Normal user login from DB
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getPassword().equals(password)) {
                // Default role assignment
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

    //  Used for OTP verification and forgot password
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email));
    }

    //  Used for profile update
    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    //  Save updated user details
    public void save(User user) {
        userRepository.save(user);
    }
}
