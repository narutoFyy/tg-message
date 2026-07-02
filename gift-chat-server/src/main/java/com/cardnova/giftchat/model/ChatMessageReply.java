package com.cardnova.giftchat.model;

public record ChatMessageReply(
    String messageId,
    String author,
    String content
) {
}
