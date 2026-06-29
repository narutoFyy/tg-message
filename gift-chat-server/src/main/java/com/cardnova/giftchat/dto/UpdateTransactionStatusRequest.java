package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateTransactionStatusRequest(
    @NotBlank String status
) {
}
