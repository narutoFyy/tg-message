package com.cardnova.giftchat.model;

public record UserAccount(
    String id,
    String username,
    String email,
    String phone,
    String role
) {
}
