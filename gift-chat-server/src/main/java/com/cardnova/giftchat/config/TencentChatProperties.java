package com.cardnova.giftchat.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.tencent.chat")
public record TencentChatProperties(
    boolean enabled,
    String sdkAppId,
    String secretKey,
    String adminIdentifier,
    String adminUserSig,
    long userSigExpireSeconds,
    String restBaseUrl
) {
    public String restBaseUrl() {
        return restBaseUrl == null || restBaseUrl.isBlank() ? "https://console.tim.qq.com" : restBaseUrl.replaceAll("/+$", "");
    }

    public String adminIdentifier() {
        return adminIdentifier == null || adminIdentifier.isBlank() ? "administrator" : adminIdentifier.trim();
    }

    public long userSigExpireSeconds() {
        return userSigExpireSeconds > 0 ? userSigExpireSeconds : 604800L;
    }

    public boolean configured() {
        return enabled
            && sdkAppId != null
            && !sdkAppId.isBlank()
            && (!isBlank(adminUserSig) || !isBlank(secretKey));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
