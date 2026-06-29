package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateFriendRequest(
    @NotBlank String username
) {
}
