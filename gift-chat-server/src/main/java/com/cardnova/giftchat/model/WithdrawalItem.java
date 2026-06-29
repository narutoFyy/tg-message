package com.cardnova.giftchat.model;

public record WithdrawalItem(
    String id,
    String requestNo,
    String ownerUsername,
    String amount,
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
