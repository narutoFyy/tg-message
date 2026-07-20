package com.cardnova.giftchat.model;

public record WithdrawalItem(
    String id,
    String requestNo,
    String sourceType,
    String ownerUsername,
    String lotteryRecordId,
    String prizeName,
    String prizeType,
    String amount,
    String currencyCode,
    String country,
    String accountName,
    String bankName,
    String accountNumber,
    String contact,
    String note,
    String status,
    String assignedAgent,
    String createdAt,
    String updatedAt
) {
}
