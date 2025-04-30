package com.professionalloan.management.controller;

import com.professionalloan.management.model.User;
import com.professionalloan.management.service.UserService;
import com.professionalloan.management.service.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "http://localhost:5173", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE})
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailService emailService;

    // --- Registration Endpoint  ---
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        boolean success = userService.registerUser(user);
        if (success) {
            return ResponseEntity.ok("Registration successful!");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Registration failed!");
        }
    }

    // --- Login Endpoint ---
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody User loginRequest) {
        User loggedInUser = userService.findByEmailAndPassword(
                loginRequest.getEmail(),
                loginRequest.getPassword()
        );

        if (loggedInUser != null) {
            Map<String, Object> response = new HashMap<>();
            response.put("id", loggedInUser.getId());
            response.put("name", loggedInUser.getName());
            response.put("email", loggedInUser.getEmail());
            response.put("role", loggedInUser.getRole());

            // Add a special flag for admin
            if ("ADMIN".equals(loggedInUser.getRole())) {
                response.put("isAdmin", true);
            }

            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }

    // --- OTP Password Reset Endpoints ---

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {
        User user = userService.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        String otp = String.format("%06d", new Random().nextInt(999999));
        user.setOtp(otp);
        user.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
        userService.save(user);

        emailService.sendOtpEmail(user.getEmail(), otp);
        return ResponseEntity.ok("OTP sent to your email.");
    }
    
    //Verify otp

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@RequestParam String email, @RequestParam String otp) {
        User user = userService.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        if (user.getOtp() != null && user.getOtp().equals(otp)
                && user.getOtpExpiry() != null && user.getOtpExpiry().isAfter(LocalDateTime.now())) {
            return ResponseEntity.ok("OTP is valid.");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired OTP.");
        }
    }

//id for specific user 
    @GetMapping("/{id}")
public ResponseEntity<?> getUser(@PathVariable Long id) {
    Optional<User> userOpt = userService.findById(id);
    if (userOpt.isEmpty()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
    }
    return ResponseEntity.ok(userOpt.get());
}

//profile update change name 
    @PutMapping("/{id}")
public ResponseEntity<?> updateUser(@PathVariable Long id, @RequestBody User updatedUser) {
    Optional<User> userOpt = userService.findById(id);
    if (userOpt.isEmpty()) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
    }
    User user = userOpt.get();
    user.setName(updatedUser.getName());
    if (updatedUser.getPassword() != null && !updatedUser.getPassword().isEmpty()) {
        user.setPassword(updatedUser.getPassword());
    }
    userService.save(user);
    return ResponseEntity.ok(user);
}

    
    //reset password 
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestParam String email,
                                           @RequestParam String otp,
                                           @RequestParam String newPassword) {
        User user = userService.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        }
        if (user.getOtp() != null && user.getOtp().equals(otp)
                && user.getOtpExpiry() != null && user.getOtpExpiry().isAfter(LocalDateTime.now())) {
            user.setPassword(newPassword); 
            user.setOtpExpiry(null);
            userService.save(user);
            return ResponseEntity.ok("Password reset successful.");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid or expired OTP.");
        }
    }
}