package com.cardnova.giftchat.model;

public record CountryCodeRule(
    String countryCode,
    String countryName,
    int minLocalLength,
    int maxLocalLength,
    boolean enabled,
    int sortOrder
) {
}
