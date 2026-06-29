package com.cardnova.giftchat.model;

import java.util.List;

public record FriendProfile(
    String id,
    String username,
    String displayName,
    String phone,
    String status,
    List<String> tags,
    List<ChatMessage> messages,
    int unreadCount,
    String blockedAt
) {
}
