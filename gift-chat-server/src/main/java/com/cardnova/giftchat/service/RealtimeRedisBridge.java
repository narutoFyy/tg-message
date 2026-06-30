package com.cardnova.giftchat.service;

import com.cardnova.giftchat.config.RealtimeRedisProperties;
import com.cardnova.giftchat.model.ChatMessage;
import com.cardnova.giftchat.model.RealtimeFanoutEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RealtimeRedisBridge {

    private final String nodeId = UUID.randomUUID().toString();
    private final RealtimeRedisProperties properties;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    private final ObjectProvider<RealtimeChatService> realtimeChatServiceProvider;

    public RealtimeRedisBridge(
        RealtimeRedisProperties properties,
        ObjectMapper objectMapper,
        ObjectProvider<StringRedisTemplate> redisTemplateProvider,
        ObjectProvider<RealtimeChatService> realtimeChatServiceProvider
    ) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.redisTemplateProvider = redisTemplateProvider;
        this.realtimeChatServiceProvider = realtimeChatServiceProvider;
    }

    public String nodeId() {
        return nodeId;
    }

    public boolean enabled() {
        return properties.enabled();
    }

    public void publish(RealtimeFanoutEvent event) {
        if (!enabled()) {
            return;
        }
        StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
        if (redisTemplate == null) {
            return;
        }
        try {
            redisTemplate.convertAndSend(properties.channel(), objectMapper.writeValueAsString(event));
        } catch (RuntimeException ignored) {
            // Local WebSocket delivery already happened; Redis fanout is best-effort.
        } catch (Exception ignored) {
            // Local WebSocket delivery already happened; Redis fanout is best-effort.
        }
    }

    public void handleMessage(String message) {
        try {
            RealtimeFanoutEvent event = objectMapper.readValue(message, RealtimeFanoutEvent.class);
            if (nodeId.equals(event.nodeId())) {
                return;
            }
            RealtimeChatService realtimeChatService = realtimeChatServiceProvider.getIfAvailable();
            if (realtimeChatService == null) {
                return;
            }
            if (event.authorAware()) {
                realtimeChatService.broadcastAuthorAwareLocal(
                    event.channelKey(),
                    event.senderUserId(),
                    event.selfAuthor(),
                    event.otherAuthor(),
                    objectMapper.convertValue(event.payload(), ChatMessage.class)
                );
                return;
            }
            realtimeChatService.broadcastPayloadLocal(event.channelKey(), event.payload());
        } catch (Exception ignored) {
            // Ignore malformed fanout messages from other nodes.
        }
    }
}
