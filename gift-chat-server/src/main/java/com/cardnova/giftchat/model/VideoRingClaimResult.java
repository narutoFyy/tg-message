package com.cardnova.giftchat.model;

public record VideoRingClaimResult(
    String sessionId,
    boolean claimed,
    String deviceId,
    String deviceType
) {
}
