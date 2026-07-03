package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Size(min = 3, max = 32) String username,
    @Size(max = 254) String email,
    @Size(max = 32) String phone,
    @NotBlank @Size(min = 8, max = 128) String password,
    @Size(max = 32) String inviteCode
) {
}
