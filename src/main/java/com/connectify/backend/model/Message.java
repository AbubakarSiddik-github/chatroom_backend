package com.connectify.backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document(collection = "messages")
@Data
public class Message {
    @Id
    private String id;
    private String roomId;
    private String senderId;
    private String senderName;
    private String content;
    private String type; // TEXT, IMAGE, FILE
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String publicId;
    private Instant createdAt = Instant.now();
    private boolean edited;
    private boolean deleted;

    // Read receipts
    private List<String> readBy = new ArrayList<>();
    private Instant lastReadAt;

    // Reply-to
    private String replyToId;
    private String replyToContent;
    private String replyToSenderName;
}
