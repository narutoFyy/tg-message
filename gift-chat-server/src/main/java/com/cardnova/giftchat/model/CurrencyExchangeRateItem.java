package com.cardnova.giftchat.model;

public record CurrencyExchangeRateItem(
    String id,
    String countryCode,
    String countryName,
    String currencyCode,
    String currencySymbol,
    String localCurrencyPerUsd,
    String displayRate,
    boolean enabled,
    String note,
    String updatedAt,
    String updatedBy
) {
}
