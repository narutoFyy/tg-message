package com.cardnova.giftchat.model;

public record BroadcastItem(
    String id,
    String senderUsername,
    String senderRole,
    String scope,
    String messageType,
    String content,
    int deliveredCount,
    String countryCodes,
    String searchKeyword,
    String targetMode,
    String targetUsernames,
    String createdAt
) {
}
