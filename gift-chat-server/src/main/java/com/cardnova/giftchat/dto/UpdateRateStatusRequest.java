package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateRateStatusRequest(
    @NotBlank String status
) {
}
