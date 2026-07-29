package com.cardnova.giftchat.model;

public record RegistrationBonusRecordItem(
    String id,
    String username,
    String phone,
    String countryCode,
    String countryName,
    String currencyCode,
    String bonusAmount,
    String status,
    String reason,
    String createdAt,
    String unlockedAt,
    String unlockedByOrderNo
) {
}
