package com.cardnova.giftchat.model;

import java.util.Map;

public record RateItem(
    String id,
    String cardName,
    String cardCode,
    String region,
    String currencyCode,
    String localPayoutPerUsd,
    Map<String, String> quotes,
    String imageUrl,
    String rate,
    String status,
    String updatedAt
) {
}
