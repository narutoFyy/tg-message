package com.cardnova.giftchat.model;

public record CountryCodeRule(
    String code,
    String countryCode,
    String countryName,
    String currencyCode,
    String currencySymbol,
    int minLocalLength,
    int maxLocalLength,
    boolean enabled,
    int sortOrder
) {
}
