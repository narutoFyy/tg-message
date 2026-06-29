package com.cardnova.giftchat.config;

import com.cardnova.giftchat.service.RealtimeChatService;
import com.cardnova.giftchat.service.UserPresenceService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final RealtimeChatService realtimeChatService;
    private final UserPresenceService userPresenceService;
    private final ApplicationEventPublisher eventPublisher;

    public ChatWebSocketHandler(
        RealtimeChatService realtimeChatService,
        UserPresenceService userPresenceService,
        ApplicationEventPublisher eventPublisher
    ) {
        this.realtimeChatService = realtimeChatService;
        this.userPresenceService = userPresenceService;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        if (userPresenceService.register(session)) {
            eventPublisher.publishEvent(new PresenceChangedEvent(userId(session), true));
        }
        realtimeChatService.register(channelKey(session), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        realtimeChatService.unregister(channelKey(session), session);
        if (userPresenceService.unregister(session)) {
            eventPublisher.publishEvent(new PresenceChangedEvent(userId(session), false));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        if ("ping".equalsIgnoreCase(message.getPayload())) {
            session.sendMessage(new TextMessage("pong"));
        }
    }

    private String channelKey(WebSocketSession session) {
        String path = session.getUri() == null ? "" : session.getUri().getPath();
        String[] parts = path.split("/");
        if (parts.length < 5) {
            return "unknown";
        }
        String type = parts[3];
        String id = parts[4];
        return type + ":" + id;
    }

    private String userId(WebSocketSession session) {
        Object userId = session.getAttributes().get("userId");
        return userId == null ? "" : String.valueOf(userId);
    }

    public record PresenceChangedEvent(String userId, boolean online) {
    }
}
