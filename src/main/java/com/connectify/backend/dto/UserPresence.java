package com.connectify.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserPresence {
    private String userId;
    private String username;
    private String status;   // ONLINE / OFFLINE
    private String lastSeen;
}
