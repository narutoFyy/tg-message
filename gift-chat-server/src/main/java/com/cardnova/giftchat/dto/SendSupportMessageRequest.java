package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;

public record SendSupportMessageRequest(
    @NotBlank String content,
    @NotBlank String messageType,
    String clientMessageId
) {
}
