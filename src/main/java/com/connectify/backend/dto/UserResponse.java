package com.connectify.backend.dto;

import lombok.Data;

@Data
public class UserResponse {
    private String id;
    private String username;
    private String email;
    private String avatarUrl;
    private String bio;
    private boolean active;
}
