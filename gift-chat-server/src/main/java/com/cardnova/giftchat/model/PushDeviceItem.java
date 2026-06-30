package com.cardnova.giftchat.model;

public record PushDeviceItem(
    String id,
    String platform,
    String provider,
    String deviceModel,
    String appVersion,
    boolean enabled,
    String lastSeenAt
) {
}
