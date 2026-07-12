package com.cardnova.giftchat.model;

public record CompletedTransactionFeedItem(
    String displayName,
    String cardName,
    String payoutAmount,
    String completedAt
) {
}
