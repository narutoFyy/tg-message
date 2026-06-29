package com.cardnova.giftchat.model;

public record LoanApplicationItem(
    String id,
    String applicationNo,
    String ownerUsername,
    String amount,
    String country,
    String purpose,
    String contact,
    String repaymentPlan,
    String status,
    String reviewNote,
    String assignedAgent,
    String reviewer,
    String createdAt,
    String updatedAt
) {
}
