package com.cardnova.giftchat.model;

public record SupportCustomerSearchResult(
    String conversationId,
    String customerUsername,
    String displayName,
    String phone,
    String phoneCountryCode,
    String email,
    String vipLevel,
    String vipPoints,
    int unreadCount,
    String lastMessageTime,
    boolean online
) {
}
