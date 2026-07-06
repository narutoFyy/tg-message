package com.cardnova.giftchat.model;

public record RegistrationBonusConfigItem(
    String id,
    String countryCode,
    String countryName,
    String currencyCode,
    String bonusAmount,
    boolean enabled,
    String note,
    String updatedAt,
    String updatedBy
) {
}
