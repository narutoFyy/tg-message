package com.cardnova.giftchat.model;

public record RateItem(
    String id,
    String cardName,
    String cardCode,
    String region,
    String currencyCode,
    String localPayoutPerUsd,
    String rate,
    String status,
    String updatedAt
) {
}
