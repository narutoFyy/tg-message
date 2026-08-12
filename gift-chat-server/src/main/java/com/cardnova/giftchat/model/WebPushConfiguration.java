package com.cardnova.giftchat.model;

public record WebPushConfiguration(
    boolean enabled,
    String publicKey
) {
}
