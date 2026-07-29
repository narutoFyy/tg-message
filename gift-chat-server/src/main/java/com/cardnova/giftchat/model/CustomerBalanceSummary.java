package com.cardnova.giftchat.model;

public record CustomerBalanceSummary(
    String availableTotal,
    String lockedTotal,
    String pendingTotal,
    String pendingWithdrawalTotal,
    String withdrawnTotal
) {
}
