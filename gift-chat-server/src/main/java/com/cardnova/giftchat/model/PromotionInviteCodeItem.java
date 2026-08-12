package com.cardnova.giftchat.model;

public record PromotionInviteCodeItem(
    String code,
    String type,
    boolean enabled,
    long registrationCount,
    String createdBy,
    String createdAt,
    String updatedAt
) {
}
