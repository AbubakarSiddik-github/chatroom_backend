package com.connectify.backend.dto;

import lombok.Data;
import java.util.List;

@Data
public class RoomRequest {
    private String name;
    private String type;
    private List<String> memberIds;
    private List<String> adminIds;
    private String avatarUrl;
}
