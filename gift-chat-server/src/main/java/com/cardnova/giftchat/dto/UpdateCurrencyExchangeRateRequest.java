package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateCurrencyExchangeRateRequest(
    @NotBlank @Size(max = 2) String countryCode,
    @NotNull @DecimalMin(value = "0.000001") BigDecimal localCurrencyPerUsd,
    boolean enabled,
    @Size(max = 255) String note
) {
}
