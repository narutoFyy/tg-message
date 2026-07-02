package com.cardnova.giftchat.model;

public record SupportCustomerInfo(
    String id,
    String username,
    String avatarUrl,
    String email,
    String phone,
    String status,
    String agentNote,
    boolean online,
    String assignedAgent,
    String createdAt,
    String updatedAt
) {
}
