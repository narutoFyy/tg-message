package com.cardnova.giftchat.model;

public record LotteryEligibility(
    String vipLevel,
    String accessStatus,
    boolean eligible,
    String periodType,
    String periodKey,
    long periodDrawCount,
    long availableChances,
    String nextAvailableAt,
    String message
) {
}
