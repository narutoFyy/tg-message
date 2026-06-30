package com.cardnova.giftchat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.realtime.redis")
public record RealtimeRedisProperties(
    boolean enabled,
    String channel
) {
    public String channel() {
        return channel == null || channel.isBlank() ? "gift-chat:realtime" : channel;
    }
}
