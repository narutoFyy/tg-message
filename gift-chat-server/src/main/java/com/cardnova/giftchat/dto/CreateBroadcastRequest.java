package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateBroadcastRequest(
    @NotBlank String scope,
    @NotBlank String content,
    @NotBlank String messageType
) {
}
