package com.cardnova.giftchat.model;

public record AdminUserItem(
    String id,
    String username,
    String email,
    String phone,
    String role,
    String status,
    boolean blacklisted,
    String createdAt
) {
}
