package com.cardnova.giftchat.model;

public record SupportLedgerCustomer(
    String conversationId,
    String customerUsername,
    String displayName,
    String assignedAgent,
    String availableTotal,
    String lockedTotal,
    String pendingTotal,
    String pendingWithdrawalTotal,
    String withdrawnTotal,
    int orderCount,
    int pendingOrderCount,
    int withdrawalCount,
    String updatedAt
) {
}
