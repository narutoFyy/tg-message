package com.cardnova.giftchat.service;

import com.cardnova.giftchat.model.ChatMessage;
import com.cardnova.giftchat.model.RealtimeFanoutEvent;
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
    private final RealtimeRedisBridge realtimeRedisBridge;
    private final Map<String, Set<WebSocketSession>> channelSessions = new ConcurrentHashMap<>();
    private final Map<String, Object> sessionSendLocks = new ConcurrentHashMap<>();

    public RealtimeChatService(ObjectMapper objectMapper, RealtimeRedisBridge realtimeRedisBridge) {
        this.objectMapper = objectMapper;
        this.realtimeRedisBridge = realtimeRedisBridge;
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
        sessionSendLocks.remove(session.getId());
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
        broadcast(channelKey, senderUserId, selfAuthor, otherAuthor, messageType, content, messageId, createdAt, "", 0L, "delivered", "", "");
    }

    public void broadcast(
        String channelKey,
        String senderUserId,
        String selfAuthor,
        String otherAuthor,
        String messageType,
        String content,
        String messageId,
        String createdAt,
        String clientMessageId,
        Long serverSeq,
        String deliveryStatus,
        String deliveredAt,
        String failedReason
    ) {
        ChatMessage payload = new ChatMessage(
            messageId,
            otherAuthor,
            messageType,
            content,
            createdAt,
            "sent",
            clientMessageId == null ? "" : clientMessageId,
            serverSeq == null ? 0L : serverSeq,
            deliveryStatus == null || deliveryStatus.isBlank() ? "delivered" : deliveryStatus.toLowerCase(),
            deliveredAt == null ? "" : deliveredAt,
            failedReason == null ? "" : failedReason
        );
        broadcastAuthorAwareLocal(channelKey, senderUserId, selfAuthor, otherAuthor, payload);
        publish(channelKey, senderUserId, payload, true, selfAuthor, otherAuthor);
    }

    public void broadcastReadReceipt(
        String channelKey,
        String channelType,
        String channelId,
        String readerUserId,
        String readerUsername,
        String readAt
    ) {
        Map<String, String> payload = Map.of(
            "eventType", "read",
            "channelType", channelType,
            "channelId", channelId,
            "readerUserId", readerUserId,
            "readerUsername", readerUsername,
            "readAt", readAt
        );
        broadcastPayloadLocal(channelKey, payload);
        publish(channelKey, null, payload, false, "", "");
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

    public void broadcastVideoSessionStatus(
        String channelKey,
        String channelType,
        String channelId,
        String sessionId,
        String roomId,
        String status,
        String startedAt,
        String endedAt,
        String updatedAt
    ) {
        Map<String, String> payload = Map.of(
            "eventType", "video_session_status",
            "channelType", channelType,
            "channelId", channelId,
            "sessionId", sessionId,
            "roomId", roomId,
            "status", status,
            "startedAt", startedAt,
            "endedAt", endedAt,
            "updatedAt", updatedAt
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
        broadcastPayloadLocal(channelKey, payload);
        publish(channelKey, null, payload, false, "", "");
    }

    public void broadcastPayloadLocal(String channelKey, Object payload) {
        Set<WebSocketSession> sessions = channelSessions.get(channelKey);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        sessions.removeIf(session -> !session.isOpen());
        for (WebSocketSession session : sessions) {
            sendSafely(session, payload);
        }
    }

    public void broadcastAuthorAwareLocal(
        String channelKey,
        String senderUserId,
        String selfAuthor,
        String otherAuthor,
        Object payload
    ) {
        Set<WebSocketSession> sessions = channelSessions.get(channelKey);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }

        sessions.removeIf(session -> !session.isOpen());
        for (WebSocketSession session : sessions) {
            String viewerUserId = (String) session.getAttributes().get("userId");
            Object sessionPayload = payload;
            if (payload instanceof ChatMessage message) {
                String author = senderUserId != null && senderUserId.equals(viewerUserId) ? selfAuthor : otherAuthor;
                sessionPayload = new ChatMessage(
                    message.id(),
                    author,
                    message.type(),
                    message.content(),
                    message.createdAt(),
                    message.readState(),
                    message.clientMessageId(),
                    message.serverSeq(),
                    message.deliveryStatus(),
                    message.deliveredAt(),
                    message.failedReason()
                );
            }
            sendSafely(session, sessionPayload);
        }
    }

    private void publish(
        String channelKey,
        String senderUserId,
        Object payload,
        boolean authorAware,
        String selfAuthor,
        String otherAuthor
    ) {
        realtimeRedisBridge.publish(new RealtimeFanoutEvent(
            realtimeRedisBridge.nodeId(),
            channelKey,
            senderUserId,
            payload,
            authorAware,
            selfAuthor,
            otherAuthor
        ));
    }

    private void sendSafely(WebSocketSession session, Object payload) {
        if (!session.isOpen()) {
            return;
        }

        Object lock = sessionSendLocks.computeIfAbsent(session.getId(), ignored -> new Object());
        synchronized (lock) {
            if (!session.isOpen()) {
                return;
            }
            try {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
            } catch (IOException | RuntimeException ignored) {
                closeSession(session);
            }
        }
    }

    private void closeSession(WebSocketSession session) {
        sessionSendLocks.remove(session.getId());
        try {
            session.close();
        } catch (IOException ignoredClose) {
            // no-op
        }
    }

    public static String friendChannel(String friendshipId) {
        return "friend:" + friendshipId;
    }

    public static String supportChannel(String conversationId) {
        return "support:" + conversationId;
    }
}
