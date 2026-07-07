package com.cardnova.giftchat.config;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.service.JwtService;
import com.cardnova.giftchat.service.RealtimeChatService;
import com.cardnova.giftchat.service.UserPresenceService;
import com.cardnova.giftchat.service.WebSocketChannelAuthorizationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final String AUTHENTICATED_ATTR = "websocketAuthenticated";

    private final RealtimeChatService realtimeChatService;
    private final UserPresenceService userPresenceService;
    private final ApplicationEventPublisher eventPublisher;
    private final JwtService jwtService;
    private final WebSocketChannelAuthorizationService channelAuthorizationService;
    private final ObjectMapper objectMapper;

    public ChatWebSocketHandler(
        RealtimeChatService realtimeChatService,
        UserPresenceService userPresenceService,
        ApplicationEventPublisher eventPublisher,
        JwtService jwtService,
        WebSocketChannelAuthorizationService channelAuthorizationService,
        ObjectMapper objectMapper
    ) {
        this.realtimeChatService = realtimeChatService;
        this.userPresenceService = userPresenceService;
        this.eventPublisher = eventPublisher;
        this.jwtService = jwtService;
        this.channelAuthorizationService = channelAuthorizationService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // Authentication is sent as the first websocket message to keep JWTs out of URLs and proxy logs.
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        if (!isAuthenticated(session)) {
            return;
        }
        realtimeChatService.unregister(channelKey(session), session);
        if (userPresenceService.unregister(session)) {
            eventPublisher.publishEvent(new PresenceChangedEvent(userId(session), false));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if (!isAuthenticated(session)) {
            authenticate(session, message.getPayload());
            return;
        }
        if ("ping".equalsIgnoreCase(message.getPayload())) {
            session.sendMessage(new TextMessage("pong"));
        }
    }

    private void authenticate(WebSocketSession session, String payload) throws IOException {
        try {
            JsonNode root = objectMapper.readTree(payload);
            if (!"auth".equalsIgnoreCase(root.path("type").asText())) {
                throw new IllegalArgumentException("Missing websocket auth message");
            }
            String token = root.path("token").asText("");
            if (token.isBlank()) {
                throw new IllegalArgumentException("Missing websocket token");
            }

            DecodedJWT jwt = jwtService.verify(token.trim());
            UserEntity user = channelAuthorizationService.requireAccess(
                jwt.getSubject(),
                stringAttribute(session, "channelType"),
                stringAttribute(session, "channelId")
            );
            session.getAttributes().put("userId", user.getId());
            session.getAttributes().put("username", user.getUsername());
            session.getAttributes().put("roleCode", user.getRoleCode());
            session.getAttributes().put(AUTHENTICATED_ATTR, true);

            if (userPresenceService.register(session)) {
                eventPublisher.publishEvent(new PresenceChangedEvent(userId(session), true));
            }
            realtimeChatService.register(channelKey(session), session);
            session.sendMessage(new TextMessage("{\"type\":\"auth_ok\"}"));
        } catch (RuntimeException | IOException exception) {
            if (session.isOpen()) {
                session.close(CloseStatus.POLICY_VIOLATION);
            }
        }
    }

    private boolean isAuthenticated(WebSocketSession session) {
        return Boolean.TRUE.equals(session.getAttributes().get(AUTHENTICATED_ATTR));
    }

    private String channelKey(WebSocketSession session) {
        Object channelType = session.getAttributes().get("channelType");
        Object channelId = session.getAttributes().get("channelId");
        if (channelType == null || channelId == null) {
            return "unknown";
        }
        return String.valueOf(channelType).toLowerCase() + ":" + channelId;
    }

    private String userId(WebSocketSession session) {
        return stringAttribute(session, "userId");
    }

    private String stringAttribute(WebSocketSession session, String attributeName) {
        Object attribute = session.getAttributes().get(attributeName);
        return attribute == null ? "" : String.valueOf(attribute);
    }

    public record PresenceChangedEvent(String userId, boolean online) {
    }
}
