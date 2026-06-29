package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
    @NotBlank String username,
    String email,
    String phone,
    @NotBlank String password
) {
}
