package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeleteWebPushSubscriptionRequest(
    @NotBlank @Size(max = 2048) String endpoint
) {
}
