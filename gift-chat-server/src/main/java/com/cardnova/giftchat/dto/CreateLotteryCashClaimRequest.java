package com.cardnova.giftchat.dto;

import jakarta.validation.Valid;

public record CreateLotteryCashClaimRequest(
    @Valid BindBankAccountRequest bankAccount
) {
}
