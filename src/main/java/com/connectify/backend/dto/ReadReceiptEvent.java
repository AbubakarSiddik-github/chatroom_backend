package com.connectify.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ReadReceiptEvent {
    private String messageId;
    private String userId;
    private String username;
    private String readAt;
}
