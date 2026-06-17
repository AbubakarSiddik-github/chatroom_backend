package com.connectify.backend.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.Instant;
import java.util.List;

@Document(collection = "rooms")
@Data
public class Room {
    @Id
    private String id;
    private String name;
    private String type; // e.g., GROUP
    private List<String> memberIds;
    private List<String> adminIds;
    private String avatarUrl;
    private Instant createdAt = Instant.now();
}
