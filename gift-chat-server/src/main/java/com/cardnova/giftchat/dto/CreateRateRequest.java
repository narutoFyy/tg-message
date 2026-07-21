package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;

public record CreateRateRequest(
    @NotBlank @Size(max = 128) String cardName,
    @Size(max = 64) String cardCode,
    @NotBlank String region,
    @Size(max = 64) String rate,
    @DecimalMin(value = "0.000001") BigDecimal localPayoutPerUsd,
    Map<String, BigDecimal> quotes,
    @Size(max = 500) String imageUrl
) {
}
