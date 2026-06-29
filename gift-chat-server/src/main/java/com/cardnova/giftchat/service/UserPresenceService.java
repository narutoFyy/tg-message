package com.cardnova.giftchat.service;

import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserPresenceService {

    private final Map<String, Set<String>> userSessionIds = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> sessionsById = new ConcurrentHashMap<>();

    public boolean register(WebSocketSession session) {
        String userId = userId(session);
        if (userId == null || userId.isBlank()) {
            return false;
        }
        Set<String> sessions = userSessionIds.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet());
        boolean wasOffline = sessions.isEmpty();
        sessions.add(session.getId());
        sessionsById.put(session.getId(), session);
        return wasOffline;
    }

    public boolean unregister(WebSocketSession session) {
        String userId = userId(session);
        sessionsById.remove(session.getId());
        if (userId == null || userId.isBlank()) {
            return false;
        }

        Set<String> sessions = userSessionIds.get(userId);
        if (sessions == null) {
            return false;
        }
        sessions.remove(session.getId());
        if (sessions.isEmpty()) {
            userSessionIds.remove(userId);
            return true;
        }
        return false;
    }

    public void disconnectUser(String userId) {
        Set<String> sessionIds = userSessionIds.get(userId);
        if (sessionIds == null || sessionIds.isEmpty()) {
            return;
        }
        sessionIds.stream()
            .map(sessionsById::get)
            .filter(session -> session != null && session.isOpen())
            .forEach(session -> {
                try {
                    session.close(CloseStatus.NORMAL);
                } catch (IOException ignored) {
                    // The close callback removes presence state when the container reports it.
                }
            });
    }

    public boolean isOnline(String userId) {
        Set<String> sessions = userSessionIds.get(userId);
        return sessions != null && !sessions.isEmpty();
    }

    private String userId(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        return userId == null ? null : String.valueOf(userId);
    }
}
