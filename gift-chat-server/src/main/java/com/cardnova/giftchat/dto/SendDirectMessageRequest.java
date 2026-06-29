package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;

public record SendDirectMessageRequest(
    @NotBlank String content,
    @NotBlank String messageType
) {
}
