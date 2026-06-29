package com.cardnova.giftchat.model;

public record CustomerBalanceSummary(
    String availableTotal,
    String pendingTotal,
    String withdrawnTotal
) {
}
