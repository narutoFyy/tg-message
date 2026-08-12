package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record AdjustCustomerWalletRequest(
    @NotNull @DecimalMin(value = "0.01") @Digits(integer = 16, fraction = 2) BigDecimal amount,
    @NotBlank @Pattern(regexp = "ADD|SUBTRACT") String action,
    @NotBlank @Size(max = 255) String reason
) {
}
