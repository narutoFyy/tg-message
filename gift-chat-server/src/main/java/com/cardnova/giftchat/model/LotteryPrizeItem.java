package com.cardnova.giftchat.model;

public record LotteryPrizeItem(
    String id,
    String name,
    String prizeType,
    int weight,
    String imageUrl,
    boolean enabled,
    int sortOrder
) {
}
