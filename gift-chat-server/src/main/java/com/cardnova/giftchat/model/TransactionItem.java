package com.cardnova.giftchat.model;

public record TransactionItem(
    String id,
    String orderNo,
    String cardName,
    String faceValue,
    String payoutAmount,
    String status,
    String counterpartyName,
    String counterpartyUsername,
    String friendshipId,
    String note,
    String voucherImageUrl,
    String cancelReason,
    String cancelNote,
    String canceledBy,
    String canceledAt,
    String createdAt,
    String updatedAt
) {
}
