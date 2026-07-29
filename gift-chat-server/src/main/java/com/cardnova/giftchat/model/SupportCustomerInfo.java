package com.cardnova.giftchat.model;

public record SupportCustomerInfo(
    String id,
    String username,
    String avatarUrl,
    String email,
    String phone,
    String phoneCountryCode,
    String status,
    String agentNote,
    boolean online,
    String assignedAgent,
    String referrerUsername,
    LotteryAccessInfo lotteryAccess,
    String createdAt,
    String updatedAt
) {
}
