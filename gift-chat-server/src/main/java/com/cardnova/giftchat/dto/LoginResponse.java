package com.cardnova.giftchat.dto;

public record LoginResponse(
    String accessToken,
    String username,
    String email,
    String phone,
    String avatarUrl,
    String inviteCode,
    String invitedBy,
    String countryCode,
    String countryName,
    String currencyCode,
    String currencySymbol,
    String roleCode,
    String nextRoute,
    String expiresAt
) {
}
