package com.connectify.backend.controller;

import com.connectify.backend.dto.RoomRequest;
import com.connectify.backend.model.Room;
import com.connectify.backend.service.RoomService;
import com.connectify.backend.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final UserService userService;

    public RoomController(RoomService roomService, UserService userService) {
        this.roomService = roomService;
        this.userService = userService;
    }

    @PostMapping
    public Room createRoom(@RequestBody RoomRequest request) {
        Room room = new Room();
        room.setName(request.getName());
        room.setType(request.getType());
        room.setMemberIds(request.getMemberIds());
        room.setAdminIds(request.getAdminIds());
        room.setAvatarUrl(request.getAvatarUrl());
        return roomService.createRoom(room);
    }

    @GetMapping
    public List<Room> getAllRooms() {
        return roomService.getAllRooms();
    }

    @GetMapping("/{id}")
    public Room getRoomById(@PathVariable String id) {
        return roomService.getRoomById(id).orElse(null);
    }

    @PutMapping("/{id}")
    public Room updateRoom(@PathVariable String id, @RequestBody RoomRequest request) {
        Optional<Room> optionalRoom = roomService.getRoomById(id);
        if (optionalRoom.isPresent()) {
            Room room = optionalRoom.get();
            if (request.getName() != null) room.setName(request.getName());
            if (request.getType() != null) room.setType(request.getType());
            if (request.getMemberIds() != null) room.setMemberIds(request.getMemberIds());
            if (request.getAdminIds() != null) room.setAdminIds(request.getAdminIds());
            if (request.getAvatarUrl() != null) room.setAvatarUrl(request.getAvatarUrl());
            return roomService.updateRoom(room);
        }
        return null;
    }

    @DeleteMapping("/{id}")
    public void deleteRoom(@PathVariable String id) {
        roomService.deleteRoom(id);
    }

    /**
     * POST /api/rooms/private/{otherUserId}
     * Creates or returns an existing PRIVATE room between the authenticated user and otherUserId.
     * Current user is extracted from JWT via SecurityContext — never from request body.
     */
    @PostMapping("/private/{otherUserId}")
    public ResponseEntity<?> getOrCreatePrivateRoom(
            @PathVariable String otherUserId,
            Authentication authentication) {

        // 1. Extract current user id from JWT principal (format: "userId|username")
        String principalName = (String) authentication.getPrincipal();
        String currentUserId = principalName.split("\\|", 2)[0];

        // 2. Reject self-chat
        if (currentUserId.equals(otherUserId)) {
            return ResponseEntity.badRequest()
                    .body(java.util.Map.of("error", "Cannot create private chat with yourself"));
        }

        // 3. Verify other user exists
        if (userService.getUserById(otherUserId).isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(java.util.Map.of("error", "User not found: " + otherUserId));
        }

        // 4. Check if PRIVATE room already exists between these two users
        Optional<Room> existingRoom = roomService.findPrivateRoom(currentUserId, otherUserId);
        if (existingRoom.isPresent()) {
            return ResponseEntity.ok(existingRoom.get());
        }

        // 5. Create new PRIVATE room
        Room room = new Room();
        room.setType("PRIVATE");
        room.setName("PRIVATE");
        room.setMemberIds(Arrays.asList(currentUserId, otherUserId));
        room.setAdminIds(List.of(currentUserId));
        room.setAvatarUrl("");

        return ResponseEntity.ok(roomService.createRoom(room));
    }
}
