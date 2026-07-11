package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateLotteryFulfillmentRequest(
    @NotBlank String recipientName,
    @NotBlank String phone,
    @NotBlank String country,
    @NotBlank String addressLine
) {
}
