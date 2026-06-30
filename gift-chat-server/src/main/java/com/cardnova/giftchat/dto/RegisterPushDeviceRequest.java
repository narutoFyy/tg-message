package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterPushDeviceRequest(
    @NotBlank @Size(max = 32) String platform,
    @NotBlank @Size(max = 32) String provider,
    @NotBlank @Size(max = 255) String deviceToken,
    @Size(max = 128) String deviceModel,
    @Size(max = 32) String appVersion
) {
}
