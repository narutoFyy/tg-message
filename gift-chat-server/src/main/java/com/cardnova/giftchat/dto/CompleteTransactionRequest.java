package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CompleteTransactionRequest(
    @NotNull @DecimalMin(value = "0.01") @Digits(integer = 16, fraction = 2) BigDecimal finalLocalAmount,
    @NotNull @DecimalMin(value = "0.00") @Digits(integer = 16, fraction = 2) BigDecimal vipPoints,
    @Size(max = 255) String reason
) {
}
