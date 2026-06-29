package com.cardnova.giftchat.model;

public record AgentItem(
    String id,
    String username,
    String email,
    String phone,
    String status,
    long assignedConversationCount
) {
}
