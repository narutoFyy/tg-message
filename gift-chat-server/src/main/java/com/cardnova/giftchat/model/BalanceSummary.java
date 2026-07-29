package com.cardnova.giftchat.model;

public record BalanceSummary(
    String scope,
    String currencyCode,
    String availableTotal,
    String lockedTotal,
    String pendingTotal,
    String pendingWithdrawalTotal,
    String withdrawnTotal,
    int userCount
) {
}
