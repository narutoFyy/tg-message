package com.cardnova.giftchat.service;

import com.cardnova.giftchat.model.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RealtimeChatService {

    private final ObjectMapper objectMapper;
    private final Map<String, Set<WebSocketSession>> channelSessions = new ConcurrentHashMap<>();

    public RealtimeChatService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void register(String channelKey, WebSocketSession session) {
        channelSessions.computeIfAbsent(channelKey, ignored -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void unregister(String channelKey, WebSocketSession session) {
        Set<WebSocketSession> sessions = channelSessions.get(channelKey);
        if (sessions == null) {
            return;
        }
        sessions.remove(session);
        if (sessions.isEmpty()) {
            channelSessions.remove(channelKey);
        }
    }

    public boolean hasOpenSession(String channelKey, String userId) {
        Set<WebSocketSession> sessions = channelSessions.get(channelKey);
        if (sessions == null || sessions.isEmpty() || userId == null || userId.isBlank()) {
            return false;
        }

        sessions.removeIf(session -> !session.isOpen());
        return sessions.stream()
            .anyMatch(session -> userId.equals(session.getAttributes().get("userId")));
    }

    public void broadcast(
        String channelKey,
        String senderUserId,
        String selfAuthor,
        String otherAuthor,
        String messageType,
        String content,
        String messageId,
        String createdAt
    ) {
        Set<WebSocketSession> sessions = channelSessions.get(channelKey);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        sessions.removeIf(session -> !session.isOpen());
        for (WebSocketSession session : sessions) {
            String viewerUserId = (String) session.getAttributes().get("userId");
            String author = senderUserId != null && senderUserId.equals(viewerUserId) ? selfAuthor : otherAuthor;
            ChatMessage payload = new ChatMessage(messageId, author, messageType, content, createdAt, "sent");
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
            } catch (IOException ignored) {
                try {
                    session.close();
                } catch (IOException ignoredClose) {
                    // no-op
                }
            }
        }
    }

    public void broadcastReadReceipt(
        String channelKey,
        String channelType,
        String channelId,
        String readerUserId,
        String readerUsername,
        String readAt
    ) {
        Set<WebSocketSession> sessions = channelSessions.get(channelKey);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        Map<String, String> payload = Map.of(
            "eventType", "read",
            "channelType", channelType,
            "channelId", channelId,
            "readerUserId", readerUserId,
            "readerUsername", readerUsername,
            "readAt", readAt
        );
        sessions.removeIf(session -> !session.isOpen());
        for (WebSocketSession session : sessions) {
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
            } catch (IOException ignored) {
                try {
                    session.close();
                } catch (IOException ignoredClose) {
                    // no-op
                }
            }
        }
    }

    public void broadcastVideoInvite(String channelKey, String channelType, String channelId, String sessionId, String roomId, String initiatorUsername, String receiverUsername, String createdAt) {
        Map<String, String> payload = Map.of(
            "eventType", "video_invite",
            "channelType", channelType,
            "channelId", channelId,
            "sessionId", sessionId,
            "roomId", roomId,
            "initiatorUsername", initiatorUsername,
            "receiverUsername", receiverUsername,
            "createdAt", createdAt
        );
        broadcastPayload(channelKey, payload);
    }

    public void broadcastPresence(String channelKey, String channelType, String channelId, String userId, boolean online) {
        Map<String, Object> payload = Map.of(
            "eventType", "presence",
            "channelType", channelType,
            "channelId", channelId,
            "userId", userId,
            "online", online
        );
        broadcastPayload(channelKey, payload);
    }

    private void broadcastPayload(String channelKey, Object payload) {
        Set<WebSocketSession> sessions = channelSessions.get(channelKey);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        sessions.removeIf(session -> !session.isOpen());
        for (WebSocketSession session : sessions) {
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
            } catch (IOException ignored) {
                try {
                    session.close();
                } catch (IOException ignoredClose) {
                    // no-op
                }
            }
        }
    }

    public static String friendChannel(String friendshipId) {
        return "friend:" + friendshipId;
    }

    public static String supportChannel(String conversationId) {
        return "support:" + conversationId;
    }
}
