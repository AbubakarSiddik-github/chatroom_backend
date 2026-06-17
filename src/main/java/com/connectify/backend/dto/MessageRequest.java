package com.connectify.backend.dto;

import lombok.Data;

@Data
public class MessageRequest {
    private String roomId;
    private String senderId;
    private String content;
    private String type;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String publicId;
}
