package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateBlacklistRequest(
    @NotBlank String username,
    String reason
) {
}
