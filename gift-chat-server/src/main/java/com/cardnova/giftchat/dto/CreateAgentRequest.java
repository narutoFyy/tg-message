package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAgentRequest(
    @NotBlank String username,
    String email,
    String phone,
    @NotBlank String password
) {
}
