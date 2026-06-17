package com.connectify.backend.service;

import com.connectify.backend.dto.UserPresence;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory presence tracker.
 * Keyed by userId → UserPresence.
 * Thread-safe via ConcurrentHashMap.
 */
@Service
public class PresenceService {

    private final ConcurrentHashMap<String, UserPresence> onlineUsers = new ConcurrentHashMap<>();

    public UserPresence userOnline(String userId, String username) {
        UserPresence presence = new UserPresence(userId, username, "ONLINE", null);
        onlineUsers.put(userId, presence);
        return presence;
    }

    public UserPresence userOffline(String userId, String username) {
        String lastSeen = Instant.now().toString();
        UserPresence presence = new UserPresence(userId, username, "OFFLINE", lastSeen);
        onlineUsers.remove(userId);
        return presence;
    }

    public Collection<UserPresence> getAllOnlineUsers() {
        return onlineUsers.values();
    }

    public boolean isOnline(String userId) {
        return onlineUsers.containsKey(userId);
    }
}
