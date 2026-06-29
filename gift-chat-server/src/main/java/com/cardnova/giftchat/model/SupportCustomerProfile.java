package com.cardnova.giftchat.model;

import java.util.List;

public record SupportCustomerProfile(
    String conversationId,
    SupportCustomerInfo customer,
    CustomerBalanceSummary balance,
    List<TransactionItem> orders,
    List<WithdrawalItem> withdrawals,
    List<LoanApplicationItem> loans,
    List<VideoSessionItem> videoSessions
) {
}
