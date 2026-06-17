package com.connectify.backend.controller;

import com.connectify.backend.dto.MessageRequest;
import com.connectify.backend.dto.ReadReceiptEvent;
import com.connectify.backend.model.Message;
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

    public MessageController(MessageService messageService, SimpMessagingTemplate messagingTemplate) {
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
    }

    @PostMapping
    public Message createMessage(@RequestBody MessageRequest request) {
        Message message = new Message();
        message.setRoomId(request.getRoomId());
        message.setSenderId(request.getSenderId());
        message.setContent(request.getContent());
        message.setType(request.getType());
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
    public Message updateMessage(@PathVariable String id, @RequestBody MessageRequest request) {
        Optional<Message> optionalMessage = messageService.getMessageById(id);
        if (optionalMessage.isPresent()) {
            Message message = optionalMessage.get();
            message.setContent(request.getContent());
            message.setEdited(true);
            return messageService.updateMessage(message);
        }
        return null;
    }

    @DeleteMapping("/{id}")
    public Message deleteMessage(@PathVariable String id) {
        Optional<Message> optionalMessage = messageService.getMessageById(id);
        if (optionalMessage.isPresent()) {
            Message message = optionalMessage.get();
            message.setContent("This message was deleted");
            message.setDeleted(true);
            return messageService.updateMessage(message);
        }
        return null;
    }

    /**
     * POST /api/messages/{messageId}/read
     * Mark a message as read by the authenticated JWT user.
     * Persists readBy + lastReadAt, then broadcasts a WebSocket receipt event.
     */
    @PostMapping("/{messageId}/read")
    public ResponseEntity<?> markMessageRead(@PathVariable String messageId,
                                             Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Unauthorized"));
        }

        // Principal is stored as "userId|username" by JwtFilter
        String principal = (String) authentication.getPrincipal();
        String[] parts   = principal.split("\\|", 2);
        String userId    = parts[0];
        String username  = parts.length > 1 ? parts[1] : "unknown";

        Optional<Message> optionalMessage = messageService.getMessageById(messageId);
        if (optionalMessage.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Message not found"));
        }

        Message message = optionalMessage.get();

        // Add reader without duplicate
        if (!message.getReadBy().contains(userId)) {
            message.getReadBy().add(userId);
        }
        message.setLastReadAt(Instant.now());
        Message saved = messageService.updateMessage(message);

        // Broadcast read receipt on /topic/read/{roomId}
        String readAt = Instant.now().toString();
        ReadReceiptEvent event = new ReadReceiptEvent(messageId, userId, username, readAt);
        messagingTemplate.convertAndSend("/topic/read/" + message.getRoomId(), event);

        return ResponseEntity.ok(saved);
    }
}
