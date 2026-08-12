package com.cardnova.giftchat.model;

public record WebPushSubscriptionItem(
    String id,
    boolean enabled,
    String updatedAt
) {
}
