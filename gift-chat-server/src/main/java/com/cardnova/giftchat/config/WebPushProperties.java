package com.cardnova.giftchat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.web-push")
public record WebPushProperties(
    boolean enabled,
    String publicKey,
    String privateKey,
    String subject,
    int ttlSeconds
) {
    public boolean configured() {
        return enabled && hasText(publicKey) && hasText(privateKey) && hasText(subject);
    }

    public int ttlSeconds() {
        return ttlSeconds > 0 ? Math.min(ttlSeconds, 86_400) : 300;
    }

    public String publicKey() {
        return publicKey == null ? "" : publicKey.trim();
    }

    public String privateKey() {
        return privateKey == null ? "" : privateKey.trim();
    }

    public String subject() {
        return subject == null || subject.isBlank()
            ? "mailto:support@stonetradex.com"
            : subject.trim();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
