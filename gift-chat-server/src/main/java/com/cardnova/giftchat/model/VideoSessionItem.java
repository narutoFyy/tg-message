package com.cardnova.giftchat.model;

public record VideoSessionItem(
    String id,
    String roomId,
    String channelType,
    String channelId,
    String initiatorUsername,
    String receiverUsername,
    String vendor,
    String status,
    String startedAt,
    String endedAt,
    String createdAt,
    String updatedAt
) {
}
