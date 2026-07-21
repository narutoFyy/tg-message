package com.cardnova.giftchat.model;
import java.util.List;
public record VipBenefitSummary(
    String vipLevel,
    String birthDate,
    boolean birthdayLocked,
    boolean birthdayEligible,
    String birthdayRewardNgn,
    String birthdayRewardDisplay,
    boolean supportRedPacketEligible,
    String supportRewardDisplay,
    List<VipHolidayRewardItem> holidayRewards
) {}
