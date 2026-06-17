package com.connectify.backend.config;

import com.connectify.backend.dto.UserPresence;
import com.connectify.backend.security.JwtUtil;
import com.connectify.backend.service.PresenceService;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final JwtUtil jwtUtil;
    private final PresenceService presenceService;

    @Lazy
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public WebSocketConfig(JwtUtil jwtUtil, PresenceService presenceService) {
        this.jwtUtil = jwtUtil;
        this.presenceService = presenceService;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // Raw WebSocket endpoint for wscat/Node.js testing
        registry.addEndpoint("/ws-raw")
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

                if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
                    String authHeader = accessor.getFirstNativeHeader("Authorization");
                    if (authHeader != null && authHeader.startsWith("Bearer ")) {
                        String token = authHeader.substring(7);
                        if (jwtUtil.validateToken(token)) {
                            // Store userId:username as name — format: "userId|username"
                            String userId   = jwtUtil.extractUserId(token);
                            String username = jwtUtil.extractUsername(token);
                            String principal = userId + "|" + username;

                            UsernamePasswordAuthenticationToken auth =
                                    new UsernamePasswordAuthenticationToken(principal, null, List.of());
                            accessor.setUser(auth);
                        }
                    }
                }
                return message;
            }
        });
    }

    // ── Presence: ONLINE on connect ───────────────────────────────────────────
    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        if (principal != null) {
            String[] parts = principal.getName().split("\\|", 2);
            String userId   = parts[0];
            String username = parts.length > 1 ? parts[1] : "unknown";

            UserPresence presence = presenceService.userOnline(userId, username);
            messagingTemplate.convertAndSend("/topic/presence", presence);
        }
    }

    // ── Presence: OFFLINE on disconnect ──────────────────────────────────────
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal principal = accessor.getUser();
        if (principal != null) {
            String[] parts = principal.getName().split("\\|", 2);
            String userId   = parts[0];
            String username = parts.length > 1 ? parts[1] : "unknown";

            UserPresence presence = presenceService.userOffline(userId, username);
            messagingTemplate.convertAndSend("/topic/presence", presence);
        }
    }
}
