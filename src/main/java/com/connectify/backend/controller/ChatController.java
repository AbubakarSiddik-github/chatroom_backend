package com.connectify.backend.controller;

import com.connectify.backend.dto.ChatMessage;
import com.connectify.backend.dto.ReadReceiptEvent;
import com.connectify.backend.dto.TypingEvent;
import com.connectify.backend.dto.TypingResponse;
import com.connectify.backend.model.Message;
import com.connectify.backend.service.MessageService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.Optional;

@Controller
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final MessageService messageService;

    public ChatController(SimpMessagingTemplate messagingTemplate, MessageService messageService) {
        this.messagingTemplate = messagingTemplate;
        this.messageService = messageService;
    }

    // ── PHASE 1: Room chat ─────────────────────────────────────────────────────
    /**
     * Client sends to:  /app/chat/{roomId}
     * Server broadcasts to: /topic/room/{roomId}
     */
    @MessageMapping("/chat/{roomId}")
    public void sendMessage(@DestinationVariable String roomId,
                            @Payload ChatMessage chatMessage) {
        Message savedMessage = new Message();
        savedMessage.setRoomId(roomId);
        savedMessage.setSenderId(chatMessage.getSenderId());
        savedMessage.setSenderName(chatMessage.getSenderName());
        savedMessage.setContent(chatMessage.getContent());
        savedMessage.setType(chatMessage.getType() != null ? chatMessage.getType() : "TEXT");
        savedMessage.setCreatedAt(Instant.now());
        savedMessage.setEdited(false);
        savedMessage.setDeleted(false);

        Message persisted = messageService.createMessage(savedMessage);
        messagingTemplate.convertAndSend("/topic/room/" + roomId, persisted);
    }

    @MessageMapping("/join/{roomId}")
    public void joinRoom(@DestinationVariable String roomId, @Payload ChatMessage chatMessage) {
        chatMessage.setType("JOIN");
        chatMessage.setContent(chatMessage.getSenderName() + " joined the room");
        messagingTemplate.convertAndSend("/topic/room/" + roomId, chatMessage);
    }

    @MessageMapping("/leave/{roomId}")
    public void leaveRoom(@DestinationVariable String roomId, @Payload ChatMessage chatMessage) {
        chatMessage.setType("LEAVE");
        chatMessage.setContent(chatMessage.getSenderName() + " left the room");
        messagingTemplate.convertAndSend("/topic/room/" + roomId, chatMessage);
    }

    // ── PHASE 2: Typing Indicator ─────────────────────────────────────────────
    /**
     * Client sends:  /app/typing/{roomId}
     * Server broadcasts: /topic/typing/{roomId}  { typing: true }
     */
    @MessageMapping("/typing/{roomId}")
    public void userTyping(@DestinationVariable String roomId,
                           @Payload TypingEvent event) {
        TypingResponse response = new TypingResponse(
                event.getUserId(),
                event.getUsername(),
                true,
                Instant.now().toString()
        );
        messagingTemplate.convertAndSend("/topic/typing/" + roomId, response);
    }

    /**
     * Client sends:  /app/typing-stop/{roomId}
     * Server broadcasts: /topic/typing/{roomId}  { typing: false }
     */
    @MessageMapping("/typing-stop/{roomId}")
    public void userStopTyping(@DestinationVariable String roomId,
                               @Payload TypingEvent event) {
        TypingResponse response = new TypingResponse(
                event.getUserId(),
                event.getUsername(),
                false,
                Instant.now().toString()
        );
        messagingTemplate.convertAndSend("/topic/typing/" + roomId, response);
    }

    // ── PHASE 3: Read Receipt (WebSocket path) ────────────────────────────────
    /**
     * Client sends:  /app/read/{messageId}
     * Server persists readBy + broadcasts to /topic/read/{roomId}
     * Principal carries userId|username from JWT.
     */
    @MessageMapping("/read/{messageId}")
    public void markRead(@DestinationVariable String messageId,
                         Principal principal) {

        if (principal == null) return;

        String[] parts = principal.getName().split("\\|", 2);
        String userId   = parts[0];
        String username = parts.length > 1 ? parts[1] : "unknown";

        Optional<Message> optionalMessage = messageService.getMessageById(messageId);
        if (optionalMessage.isEmpty()) return;

        Message message = optionalMessage.get();

        // Add user to readBy without duplicates
        if (!message.getReadBy().contains(userId)) {
            message.getReadBy().add(userId);
        }
        message.setLastReadAt(Instant.now());
        messageService.updateMessage(message);

        // Broadcast receipt to the room
        ReadReceiptEvent event = new ReadReceiptEvent(
                messageId, userId, username, Instant.now().toString()
        );
        messagingTemplate.convertAndSend("/topic/read/" + message.getRoomId(), event);
    }
}
