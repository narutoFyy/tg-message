package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record WebPushKeysRequest(
    @NotBlank @Size(max = 255) String p256dh,
    @NotBlank @Size(max = 255) String auth
) {
}
