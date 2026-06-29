package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateLoanStatusRequest(
    @NotBlank String status,
    String reviewNote
) {
}
