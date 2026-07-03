package com.cardnova.giftchat.model;

public record ReferralRewardItem(
    String id,
    String referrerUsername,
    String referredUsername,
    String tradeOrderNo,
    String rewardType,
    String amount,
    String ratePercent,
    String status,
    String createdAt
) {
}
