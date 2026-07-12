package com.cardnova.giftchat.model;

public record LotteryPrizeItem(
    String id,
    String name,
    String prizeType,
    String baseAmountUsd,
    String localAmount,
    String currencyCode,
    String displayAmount,
    String exchangeRate,
    int weight,
    String imageUrl,
    boolean enabled,
    int sortOrder
) {
}
