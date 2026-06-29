package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateVideoSessionRequest(
    @NotBlank String channelType,
    @NotBlank String channelId
) {
}
