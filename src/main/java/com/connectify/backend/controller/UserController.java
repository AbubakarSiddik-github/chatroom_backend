package com.connectify.backend.controller;

import com.connectify.backend.dto.UploadResponse;
import com.connectify.backend.dto.UserResponse;
import com.connectify.backend.model.User;
import com.connectify.backend.service.CloudinaryService;
import com.connectify.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final CloudinaryService cloudinaryService;

    public UserController(UserService userService, CloudinaryService cloudinaryService) {
        this.userService = userService;
        this.cloudinaryService = cloudinaryService;
    }

    // ── Map helper ────────────────────────────────────────────────────────────
    private UserResponse mapToResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setAvatarUrl(user.getAvatarUrl());
        response.setBio(user.getBio());
        response.setActive(user.isActive());
        return response;
    }

    // ── Public list / get ─────────────────────────────────────────────────────
    @GetMapping
    public List<UserResponse> getAllUsers() {
        return userService.getAllUsers().stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public UserResponse getUserById(@PathVariable String id) {
        return userService.getUserById(id).map(this::mapToResponse).orElse(null);
    }

    // ── Current user — GET /api/users/me ─────────────────────────────────────
    @GetMapping("/me")
    public ResponseEntity<?> getMe(Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));

        String userId = extractUserId(authentication);
        return userService.getUserById(userId)
                .map(u -> ResponseEntity.ok(mapToResponse(u)))
                .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // ── Update profile — PUT /api/users/me/profile ───────────────────────────
    @PutMapping("/me/profile")
    public ResponseEntity<?> updateProfile(@RequestBody Map<String, String> body,
                                            Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));

        String userId = extractUserId(authentication);
        Optional<User> optUser = userService.getUserById(userId);
        if (optUser.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));

        User user = optUser.get();
        if (body.containsKey("bio")) user.setBio(body.get("bio"));
        if (body.containsKey("username")) {
            String newUsername = body.get("username");
            if (!newUsername.equals(user.getUsername()) && userService.usernameExists(newUsername))
                return ResponseEntity.badRequest().body(Map.of("error", "Username already taken"));
            if (newUsername != null && !newUsername.isBlank()) user.setUsername(newUsername);
        }
        return ResponseEntity.ok(mapToResponse(userService.updateUser(user)));
    }

    // ── Upload avatar — POST /api/users/me/avatar ─────────────────────────────
    @PostMapping("/me/avatar")
    public ResponseEntity<?> uploadAvatar(@RequestParam("file") MultipartFile file,
                                           Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));

        String userId = extractUserId(authentication);
        Optional<User> optUser = userService.getUserById(userId);
        if (optUser.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));

        User user = optUser.get();

        try {
            // Delete old avatar from Cloudinary if exists
            if (user.getAvatarPublicId() != null && !user.getAvatarPublicId().isBlank()) {
                cloudinaryService.deleteByPublicId(user.getAvatarPublicId(), "image");
            }

            UploadResponse uploaded = cloudinaryService.uploadAvatar(file);
            user.setAvatarUrl(uploaded.getUrl());
            user.setAvatarPublicId(uploaded.getPublicId());
            userService.updateUser(user);

            Map<String, String> result = new HashMap<>();
            result.put("avatarUrl", uploaded.getUrl());
            result.put("publicId", uploaded.getPublicId());
            return ResponseEntity.ok(result);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload avatar"));
        }
    }

    // ── Delete avatar — DELETE /api/users/me/avatar ───────────────────────────
    @DeleteMapping("/me/avatar")
    public ResponseEntity<?> deleteAvatar(Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));

        String userId = extractUserId(authentication);
        Optional<User> optUser = userService.getUserById(userId);
        if (optUser.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "User not found"));

        User user = optUser.get();
        if (user.getAvatarPublicId() != null && !user.getAvatarPublicId().isBlank()) {
            cloudinaryService.deleteByPublicId(user.getAvatarPublicId(), "image");
        }
        user.setAvatarUrl(null);
        user.setAvatarPublicId(null);
        userService.updateUser(user);
        return ResponseEntity.ok(Map.of("message", "Avatar deleted"));
    }

    // ── Legacy update / delete (kept for compatibility) ───────────────────────
    @PutMapping("/{id}")
    public UserResponse updateUser(@PathVariable String id, @RequestBody User request) {
        Optional<User> optionalUser = userService.getUserById(id);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (request.getUsername() != null) user.setUsername(request.getUsername());
            if (request.getEmail() != null) user.setEmail(request.getEmail());
            if (request.getAvatarUrl() != null) user.setAvatarUrl(request.getAvatarUrl());
            user.setActive(request.isActive());
            return mapToResponse(userService.updateUser(user));
        }
        return null;
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
    }

    // ── Private helper ────────────────────────────────────────────────────────
    private String extractUserId(Authentication authentication) {
        String principal = (String) authentication.getPrincipal();
        return principal.split("\\|", 2)[0];
    }
}
