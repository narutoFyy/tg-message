package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateWithdrawalStatusRequest(
    @NotBlank String status
) {
}
