package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;

public record BindBankAccountRequest(
    @NotBlank String country,
    @NotBlank String accountName,
    @NotBlank String bankName,
    @NotBlank String accountNumber
) {
}
