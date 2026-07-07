package com.cardnova.giftchat.model;

public record LotteryEligibility(
    String vipLevel,
    boolean eligible,
    String periodType,
    String periodKey,
    long periodDrawCount,
    String nextAvailableAt,
    String message
) {
}
