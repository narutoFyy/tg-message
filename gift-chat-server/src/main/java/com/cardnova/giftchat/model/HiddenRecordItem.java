package com.cardnova.giftchat.model;

public record HiddenRecordItem(
    String id,
    String targetType,
    String targetId,
    String hiddenScope,
    String createdAt,
    String restoredAt
) {
}
