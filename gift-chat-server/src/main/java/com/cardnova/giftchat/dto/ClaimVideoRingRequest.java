package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ClaimVideoRingRequest(
    @NotBlank @Size(max = 64) String deviceId,
    @NotBlank @Pattern(regexp = "mobile|desktop") String deviceType
) {
}
