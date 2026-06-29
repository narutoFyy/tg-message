package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateSellOrderRequest(
    @NotBlank String cardName,
    @NotBlank String cardCountry,
    @NotBlank String settlementCountry,
    @Positive double faceValue,
    @Positive int quantity,
    @NotBlank String rate,
    @NotBlank String settlementAmount,
    @NotBlank String cardType,
    @NotBlank String speed,
    String cardData,
    String note,
    String voucherImageUrl,
    Boolean sendChatMessage
) {
}
