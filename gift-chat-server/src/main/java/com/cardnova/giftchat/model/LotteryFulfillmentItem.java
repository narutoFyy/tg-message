package com.cardnova.giftchat.model;

public record LotteryFulfillmentItem(
    String id,
    String orderNo,
    String ownerUsername,
    String lotteryRecordId,
    String prizeName,
    String prizeType,
    String recipientName,
    String phone,
    String country,
    String addressLine,
    String status,
    String assignedAgent,
    String createdAt,
    String updatedAt
) {
}
