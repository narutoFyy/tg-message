package com.cardnova.giftchat.model;

public record FriendRequestItem(
    String friendshipId,
    String username,
    String displayName,
    String direction,
    String status,
    String createdAt
) {
}
