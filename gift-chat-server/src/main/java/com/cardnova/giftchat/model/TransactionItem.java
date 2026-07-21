package com.cardnova.giftchat.model;

public record TransactionItem(
    String id,
    String orderNo,
    String cardName,
    String faceValue,
    String faceCurrencyCode,
    String faceValueAmount,
    int quantity,
    String payoutAmount,
    String baseAmountUsd,
    String localAmount,
    String currencyCode,
    String businessRate,
    String faceToUsdRate,
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
