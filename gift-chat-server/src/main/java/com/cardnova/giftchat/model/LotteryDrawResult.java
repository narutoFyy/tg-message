package com.cardnova.giftchat.model;

public record LotteryDrawResult(
    LotteryEligibility eligibility,
    LotteryPrizeItem prize,
    String recordId,
    String drawnAt
) {
}
