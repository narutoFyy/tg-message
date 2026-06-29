package com.cardnova.giftchat.model;

import java.util.List;

public record SupportLedgerReport(
    BalanceSummary summary,
    List<SupportLedgerCustomer> customers
) {
}
