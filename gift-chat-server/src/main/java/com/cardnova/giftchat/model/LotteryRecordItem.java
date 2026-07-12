package com.cardnova.giftchat.model;

public record LotteryRecordItem(
    String id,
    String username,
    String vipLevel,
    String prizeName,
    String prizeType,
    String baseAmountUsd,
    String localAmount,
    String currencyCode,
    String displayAmount,
    String exchangeRate,
    String periodType,
    String periodKey,
    String fulfillmentStatus,
    String processedBy,
    String processedAt,
    String drawnAt
) {
}
