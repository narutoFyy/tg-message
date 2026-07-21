package com.cardnova.giftchat.model;

public record ChatOrderItem(
    String id,
    String orderNo,
    String cardName,
    String faceValue,
    String estimatedLocalAmount,
    String finalLocalAmount,
    String payoutAmount,
    String currencyCode,
    String status,
    String voucherImageUrl,
    String manualVipPoints,
    String settlementReason,
    String settledBy,
    String settledAt
) {
}
