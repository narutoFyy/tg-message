package com.cardnova.giftchat.model;

public record TencentMirrorFailureItem(
    String messageId,
    String channelType,
    String channelId,
    String content,
    String error,
    String createdAt
) {
}
