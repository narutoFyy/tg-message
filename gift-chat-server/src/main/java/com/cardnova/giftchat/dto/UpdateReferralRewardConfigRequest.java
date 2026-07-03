package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateReferralRewardConfigRequest(
    @NotNull Boolean registrationCashbackEnabled,
    @NotNull @DecimalMin("0.00") BigDecimal registrationCashbackAmount,
    @NotNull Boolean tradeRebateEnabled,
    @NotNull @DecimalMin("0.00") @DecimalMax("100.00") BigDecimal tradeRebatePercent
) {
}
