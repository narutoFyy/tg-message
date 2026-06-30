package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateWithdrawalRequest(
    @NotBlank String amount,
    @NotBlank String country,
    @NotBlank String accountName,
    @NotBlank String bankName,
    @NotBlank String accountNumber,
    String contact,
    String note,
    Boolean sendChatMessage
) {
}
