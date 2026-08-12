package com.cardnova.giftchat.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RegisterWebPushSubscriptionRequest(
    @NotBlank @Size(max = 2048) String endpoint,
    @NotNull @Valid WebPushKeysRequest keys,
    @Size(max = 255) String userAgent
) {
}
