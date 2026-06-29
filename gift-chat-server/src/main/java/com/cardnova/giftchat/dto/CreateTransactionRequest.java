package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateTransactionRequest(
    @NotBlank String counterpartyUsername,
    String friendshipId,
    @NotBlank String cardName,
    @NotBlank String faceValue,
    @NotBlank String payoutAmount,
    String note,
    String voucherImageUrl
) {
}
