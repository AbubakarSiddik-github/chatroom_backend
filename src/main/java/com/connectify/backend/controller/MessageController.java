package com.connectify.backend.controller;

import com.connectify.backend.dto.MessageRequest;
import com.connectify.backend.dto.ReadReceiptEvent;
import com.connectify.backend.model.Message;
import com.connectify.backend.service.CloudinaryService;
import com.connectify.backend.service.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;
    private final CloudinaryService cloudinaryService;

    public MessageController(MessageService messageService,
                              SimpMessagingTemplate messagingTemplate,
                              CloudinaryService cloudinaryService) {
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
        this.cloudinaryService = cloudinaryService;
    }

    @PostMapping
    public Message createMessage(@RequestBody MessageRequest request) {
        Message message = new Message();
        message.setRoomId(request.getRoomId());
        message.setSenderId(request.getSenderId());
        message.setContent(request.getContent());
        message.setType(request.getType());
        message.setFileName(request.getFileName());
        message.setFileSize(request.getFileSize());
        message.setFileType(request.getFileType());
        message.setPublicId(request.getPublicId());
        return messageService.createMessage(message);
    }

    @GetMapping("/room/{roomId}")
    public List<Message> getMessagesByRoom(@PathVariable String roomId) {
        return messageService.getMessagesByRoom(roomId);
    }

    @GetMapping("/{id}")
    public Message getMessageById(@PathVariable String id) {
        return messageService.getMessageById(id).orElse(null);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateMessage(@PathVariable String id,
                                            @RequestBody MessageRequest request,
                                            Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));

        String[] parts = ((String) authentication.getPrincipal()).split("\\|", 2);
        String userId = parts[0];

        Optional<Message> optionalMessage = messageService.getMessageById(id);
        if (optionalMessage.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Message not found"));

        Message message = optionalMessage.get();
        if (!message.getSenderId().equals(userId))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You can only edit your own messages"));

        message.setContent(request.getContent());
        message.setEdited(true);
        return ResponseEntity.ok(messageService.updateMessage(message));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMessage(@PathVariable String id,
                                            Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));

        String[] parts = ((String) authentication.getPrincipal()).split("\\|", 2);
        String userId = parts[0];

        Optional<Message> optionalMessage = messageService.getMessageById(id);
        if (optionalMessage.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Message not found"));

        Message message = optionalMessage.get();
        if (!message.getSenderId().equals(userId))
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "You can only delete your own messages"));

        // Permanently delete media from Cloudinary if present
        if (message.getPublicId() != null && !message.getPublicId().isBlank()) {
            String resourceType = "IMAGE".equals(message.getType()) ? "image" : "raw";
            cloudinaryService.deleteByPublicId(message.getPublicId(), resourceType);
        }

        // Soft-delete: clear all media data, keep placeholder text
        message.setContent("This message was deleted");
        message.setDeleted(true);
        message.setPublicId(null);
        message.setFileName(null);
        message.setFileSize(null);
        message.setFileType(null);

        Message saved = messageService.updateMessage(message);
        return ResponseEntity.ok(saved);
    }

    /**
     * POST /api/messages/{messageId}/read
     * Mark a message as read by the authenticated JWT user.
     */
    @PostMapping("/{messageId}/read")
    public ResponseEntity<?> markMessageRead(@PathVariable String messageId,
                                              Authentication authentication) {
        if (authentication == null)
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", "Unauthorized"));

        String principal = (String) authentication.getPrincipal();
        String[] parts   = principal.split("\\|", 2);
        String userId    = parts[0];
        String username  = parts.length > 1 ? parts[1] : "unknown";

        Optional<Message> optionalMessage = messageService.getMessageById(messageId);
        if (optionalMessage.isEmpty())
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", "Message not found"));

        Message message = optionalMessage.get();
        if (!message.getReadBy().contains(userId)) {
            message.getReadBy().add(userId);
        }
        message.setLastReadAt(Instant.now());
        Message saved = messageService.updateMessage(message);

        String readAt = Instant.now().toString();
        ReadReceiptEvent event = new ReadReceiptEvent(messageId, userId, username, readAt);
        messagingTemplate.convertAndSend("/topic/read/" + message.getRoomId(), event);

        return ResponseEntity.ok(saved);
    }
}
