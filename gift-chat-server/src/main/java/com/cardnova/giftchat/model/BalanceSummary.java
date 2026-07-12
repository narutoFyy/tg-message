package com.cardnova.giftchat.model;

public record BalanceSummary(
    String scope,
    String currencyCode,
    String availableTotal,
    String pendingTotal,
    String withdrawnTotal,
    int userCount
) {
}
