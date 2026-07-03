package com.cardnova.giftchat.model;

public record ReferralRewardConfigItem(
    boolean registrationCashbackEnabled,
    String registrationCashbackAmount,
    boolean tradeRebateEnabled,
    String tradeRebatePercent,
    String updatedAt,
    String updatedBy
) {
}
