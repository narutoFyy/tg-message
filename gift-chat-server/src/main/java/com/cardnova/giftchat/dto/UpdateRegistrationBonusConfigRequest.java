package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateRegistrationBonusConfigRequest(
    @NotBlank String countryCode,
    @NotBlank String countryName,
    @NotBlank String currencyCode,
    @NotNull @DecimalMin("0.00") BigDecimal bonusAmount,
    boolean enabled,
    String note
) {
}
