package com.cardnova.giftchat.model;

public record BankAccountRiskMatch(
    String riskLevel,
    String reason,
    String username,
    String displayName,
    String phoneCountryCode,
    String assignedAgent,
    String bankName,
    String accountName,
    String accountNumber,
    String submittedAt,
    boolean fullAccess
) {
}
