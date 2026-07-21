package com.cardnova.giftchat.dto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
public record UpdateVipBenefitConfigRequest(
    @NotNull @DecimalMin("0.00") BigDecimal vip4SupportAmountNgn,
    @NotNull @DecimalMin("0.00") BigDecimal vip5SupportAmountNgn,
    boolean supportRewardEnabled
) {}
