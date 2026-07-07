package com.cardnova.giftchat.model;

public record SupportMessageSearchResult(
    String conversationId,
    String messageId,
    String customerUsername,
    String displayName,
    String phoneCountryCode,
    String senderRole,
    String snippet,
    String createdAt
) {
}
