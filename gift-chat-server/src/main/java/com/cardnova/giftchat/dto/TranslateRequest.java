package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;

public record TranslateRequest(
    @NotBlank String text
) {
}
