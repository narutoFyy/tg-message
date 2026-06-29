package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRateRequest(
    @NotBlank String cardName,
    @NotBlank String region,
    @NotBlank String rate
) {
}
