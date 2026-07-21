package com.cardnova.giftchat.model;
public record VipBenefitConfigItem(
    String vip4SupportAmountNgn,
    String vip5SupportAmountNgn,
    boolean supportRewardEnabled,
    String updatedAt,
    String updatedBy
) {}
