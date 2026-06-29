package com.cardnova.giftchat.model;

public record NotificationItem(
    String id,
    String eventType,
    String title,
    String body,
    String targetType,
    String targetId,
    boolean read,
    String createdAt
) {
}
