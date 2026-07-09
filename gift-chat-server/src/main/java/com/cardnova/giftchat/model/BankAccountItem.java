package com.cardnova.giftchat.model;

public record BankAccountItem(
    String id,
    String ownerUsername,
    String country,
    String accountName,
    String bankName,
    String accountNumber,
    String maskedAccountNumber,
    String status,
    String createdAt
) {
}
