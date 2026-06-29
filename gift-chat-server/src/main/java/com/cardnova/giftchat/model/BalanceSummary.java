package com.cardnova.giftchat.model;

public record BalanceSummary(
    String scope,
    String availableTotal,
    String pendingTotal,
    String withdrawnTotal,
    int userCount
) {
}
