package com.connectify.backend.controller;

import com.connectify.backend.dto.AuthResponse;
import com.connectify.backend.dto.LoginRequest;
import com.connectify.backend.dto.RegisterRequest;
import com.connectify.backend.model.User;
import com.connectify.backend.security.JwtUtil;
import com.connectify.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {

        if (userService.emailExists(request.getEmail())) {
            return new AuthResponse(null, "Email already exists");
        }

        if (userService.usernameExists(request.getUsername())) {
            return new AuthResponse(null, "Username already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(request.getPassword());
        user.setAvatarUrl("");
        user.setActive(true);

        userService.saveUser(user);

        return new AuthResponse(null, "User registered successfully");
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        Optional<User> optionalUser = userService.findByEmailOrUsername(request.getIdentifier());
        if (optionalUser.isEmpty()) {
            return new AuthResponse(null, "User not found");
        }
        User user = optionalUser.get();
        if (!userService.checkPassword(user, request.getPassword())) {
            return new AuthResponse(null, "Invalid password");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getEmail(), user.getUsername());
        return new AuthResponse(token, "Login successful");
    }
}