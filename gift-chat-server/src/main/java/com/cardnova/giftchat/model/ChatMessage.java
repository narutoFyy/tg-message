package com.cardnova.giftchat.model;

public record ChatMessage(
    String id,
    String author,
    String type,
    String content,
    String createdAt,
    String readState
) {
}
