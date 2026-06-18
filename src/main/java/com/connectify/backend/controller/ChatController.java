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

    // ── Room chat ─────────────────────────────────────────────────────────────
    /**
     * Client sends to:       /app/chat/{roomId}
     * Server broadcasts to:  /topic/room/{roomId}
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

        // Attachment fields
        savedMessage.setFileName(chatMessage.getFileName());
        savedMessage.setFileSize(chatMessage.getFileSize());
        savedMessage.setFileType(chatMessage.getFileType());
        savedMessage.setPublicId(chatMessage.getPublicId());

        // Reply fields — look up original message for preview text
        if (chatMessage.getReplyToId() != null && !chatMessage.getReplyToId().isBlank()) {
            Optional<Message> original = messageService.getMessageById(chatMessage.getReplyToId());
            original.ifPresent(orig -> {
                savedMessage.setReplyToId(orig.getId());
                savedMessage.setReplyToSenderName(orig.getSenderName());
                // Store a short preview (first 100 chars)
                String preview = orig.getContent() != null ? orig.getContent() : "";
                if ("IMAGE".equals(orig.getType())) preview = "📷 Photo";
                else if ("FILE".equals(orig.getType())) preview = "📄 " + (orig.getFileName() != null ? orig.getFileName() : "File");
                else if (preview.length() > 100) preview = preview.substring(0, 100) + "…";
                savedMessage.setReplyToContent(preview);
            });
        }

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

    // ── Typing Indicator ──────────────────────────────────────────────────────
    @MessageMapping("/typing/{roomId}")
    public void userTyping(@DestinationVariable String roomId, @Payload TypingEvent event) {
        TypingResponse response = new TypingResponse(
                event.getUserId(), event.getUsername(), true, Instant.now().toString()
        );
        messagingTemplate.convertAndSend("/topic/typing/" + roomId, response);
    }

    @MessageMapping("/typing-stop/{roomId}")
    public void userStopTyping(@DestinationVariable String roomId, @Payload TypingEvent event) {
        TypingResponse response = new TypingResponse(
                event.getUserId(), event.getUsername(), false, Instant.now().toString()
        );
        messagingTemplate.convertAndSend("/topic/typing/" + roomId, response);
    }

    // ── Read Receipt (WebSocket path) ─────────────────────────────────────────
    @MessageMapping("/read/{messageId}")
    public void markRead(@DestinationVariable String messageId, Principal principal) {
        if (principal == null) return;

        String[] parts  = principal.getName().split("\\|", 2);
        String userId   = parts[0];
        String username = parts.length > 1 ? parts[1] : "unknown";

        Optional<Message> optionalMessage = messageService.getMessageById(messageId);
        if (optionalMessage.isEmpty()) return;

        Message message = optionalMessage.get();
        if (!message.getReadBy().contains(userId)) {
            message.getReadBy().add(userId);
        }
        message.setLastReadAt(Instant.now());
        messageService.updateMessage(message);

        ReadReceiptEvent event = new ReadReceiptEvent(
                messageId, userId, username, Instant.now().toString()
        );
        messagingTemplate.convertAndSend("/topic/read/" + message.getRoomId(), event);
    }
}
