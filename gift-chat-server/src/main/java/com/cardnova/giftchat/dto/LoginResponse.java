package com.cardnova.giftchat.dto;

public record LoginResponse(
    String accessToken,
    String username,
    String email,
    String phone,
    String roleCode,
    String nextRoute,
    String expiresAt
) {
}
