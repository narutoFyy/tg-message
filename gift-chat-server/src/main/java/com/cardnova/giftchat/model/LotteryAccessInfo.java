package com.cardnova.giftchat.model;

public record LotteryAccessInfo(
    String status,
    String approvedBy,
    String approvedAt,
    boolean canApprove,
    String message
) {
}
