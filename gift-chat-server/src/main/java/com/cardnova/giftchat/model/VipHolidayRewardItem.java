package com.cardnova.giftchat.model;
public record VipHolidayRewardItem(
    String id,
    String countryCode,
    String holidayCode,
    String holidayName,
    String holidayDate,
    String rewardAmount,
    String currencyCode,
    boolean enabled,
    boolean claimable,
    boolean claimed,
    String updatedAt,
    String updatedBy
) {}
