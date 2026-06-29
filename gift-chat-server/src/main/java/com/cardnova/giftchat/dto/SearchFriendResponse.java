package com.cardnova.giftchat.dto;

public record SearchFriendResponse(
    String username,
    String displayName,
    String status
) {
}
