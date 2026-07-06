package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;

public record CancelTransactionRequest(
    @NotBlank String reason,
    String note,
    Boolean notifyCustomer
) {
}
