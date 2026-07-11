package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateRateRequest(
    @NotBlank @Size(max = 128) String cardName,
    @Size(max = 64) String cardCode,
    @NotBlank String region,
    @NotBlank String rate
) {
}
