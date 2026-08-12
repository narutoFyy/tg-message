package com.cardnova.giftchat.model;

public record PromotionInviteRegistrationItem(
    String userId,
    String username,
    String assignedAgent,
    String registeredAt
) {
}
