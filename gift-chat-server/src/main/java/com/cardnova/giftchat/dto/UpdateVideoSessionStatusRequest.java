package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateVideoSessionStatusRequest(
    @NotBlank String status
) {
}
