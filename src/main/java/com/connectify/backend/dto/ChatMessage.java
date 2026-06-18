package com.connectify.backend.dto;

import lombok.Data;

@Data
public class ChatMessage {
    private String roomId;
    private String senderId;
    private String senderName;
    private String content;
    private String type; // TEXT, IMAGE, FILE, JOIN, LEAVE

    // Attachment fields
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String publicId;

    // Reply fields
    private String replyToId;
}
