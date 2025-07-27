package com.itransform.authservice.controller;

import com.itransform.authservice.model.User;
import com.itransform.authservice.repository.UserRepository;
import com.itransform.authservice.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody User user) {
        if (!"USER".equals(user.getRole())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Only USER role can self-register.");
        }
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Username already exists");
        }
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        return ResponseEntity.ok("User registered");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody User login) {
        Optional<User> userOpt = userRepository.findByUsername(login.getUsername());
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (passwordEncoder.matches(login.getPassword(), user.getPassword())) {
                String token = jwtUtil.generateToken(user.getUsername(), user.getRole());
                return ResponseEntity.ok(new JwtResponse(token));
            }
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
    }

    // ADMIN-only: Create MANAGER or SUPPORT user
    @PostMapping("/users/create")
    public ResponseEntity<?> createUser(@Valid @RequestBody User user, @RequestHeader(value = "Authorization", required = true) String authHeader) {
        // Validate Authorization header format
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing Authorization header");
        }
        
        try {
            String token = authHeader.substring(7);
            if (!jwtUtil.validateToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid token");
            }
            
            String role = jwtUtil.getRoleFromToken(token);
            if (!"ADMIN".equals(role)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Only ADMIN can create managers/support users");
            }
            
            if (userRepository.findByUsername(user.getUsername()).isPresent()) {
                return ResponseEntity.badRequest().body("Username already exists");
            }
            
            // Validate role is either MANAGER or SUPPORT
            if (!"MANAGER".equals(user.getRole()) && !"SUPPORT".equals(user.getRole())) {
                return ResponseEntity.badRequest().body("Only MANAGER or SUPPORT roles can be created");
            }
            
            user.setPassword(passwordEncoder.encode(user.getPassword()));
            userRepository.save(user);
            return ResponseEntity.ok("User created by admin");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Error processing token: " + e.getMessage());
        }
    }

    static class JwtResponse {
        public String token;
        public JwtResponse(String token) { this.token = token; }
    }
}
