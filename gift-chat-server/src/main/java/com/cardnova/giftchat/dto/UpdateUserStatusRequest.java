package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserStatusRequest(
    @NotBlank String status
) {
}
