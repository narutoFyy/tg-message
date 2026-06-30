package com.cardnova.giftchat.service;

import com.cardnova.giftchat.config.RealtimeRedisProperties;
import com.cardnova.giftchat.model.ChatMessage;
import com.cardnova.giftchat.model.RealtimeFanoutEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RealtimeRedisBridgeTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void ignoresEventsPublishedBySameNode() throws Exception {
        RealtimeChatService realtimeChatService = mock(RealtimeChatService.class);
        RealtimeRedisBridge bridge = bridge(realtimeChatService);
        ChatMessage payload = new ChatMessage("m1", "friend", "text", "hello", "10:00", "sent");

        bridge.handleMessage(objectMapper.writeValueAsString(new RealtimeFanoutEvent(
            bridge.nodeId(),
            "support:support-1",
            "user-1",
            payload,
            true,
            "me",
            "support"
        )));

        verify(realtimeChatService, never()).broadcastAuthorAwareLocal(any(), any(), any(), any(), any());
        verify(realtimeChatService, never()).broadcastPayloadLocal(any(), any());
    }

    @Test
    void forwardsAuthorAwareEventsFromOtherNodes() throws Exception {
        RealtimeChatService realtimeChatService = mock(RealtimeChatService.class);
        RealtimeRedisBridge bridge = bridge(realtimeChatService);
        ChatMessage payload = new ChatMessage("m1", "friend", "text", "hello", "10:00", "sent");

        bridge.handleMessage(objectMapper.writeValueAsString(new RealtimeFanoutEvent(
            "other-node",
            "support:support-1",
            "user-1",
            payload,
            true,
            "me",
            "support"
        )));

        verify(realtimeChatService).broadcastAuthorAwareLocal(
            eq("support:support-1"),
            eq("user-1"),
            eq("me"),
            eq("support"),
            any(ChatMessage.class)
        );
    }

    @SuppressWarnings("unchecked")
    private RealtimeRedisBridge bridge(RealtimeChatService realtimeChatService) {
        ObjectProvider<StringRedisTemplate> redisProvider = mock(ObjectProvider.class);
        ObjectProvider<RealtimeChatService> realtimeProvider = mock(ObjectProvider.class);
        when(realtimeProvider.getIfAvailable()).thenReturn(realtimeChatService);
        return new RealtimeRedisBridge(
            new RealtimeRedisProperties(true, "gift-chat:test"),
            objectMapper,
            redisProvider,
            realtimeProvider
        );
    }
}
