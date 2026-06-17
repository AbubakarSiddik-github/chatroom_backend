package com.connectify.backend.dto;

import lombok.Data;

@Data
public class ChatMessage {
    private String roomId;
    private String senderId;
    private String senderName;
    private String content;
    private String type; // TEXT, JOIN, LEAVE
}
