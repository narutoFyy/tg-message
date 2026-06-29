package com.cardnova.giftchat.model;

public record SupportLedgerCustomer(
    String conversationId,
    String customerUsername,
    String displayName,
    String assignedAgent,
    String availableTotal,
    String pendingTotal,
    String withdrawnTotal,
    int orderCount,
    int pendingOrderCount,
    int withdrawalCount,
    String updatedAt
) {
}
