package com.connectify.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TypingResponse {
    private String userId;
    private String username;
    private boolean typing;
    private String timestamp;
}
