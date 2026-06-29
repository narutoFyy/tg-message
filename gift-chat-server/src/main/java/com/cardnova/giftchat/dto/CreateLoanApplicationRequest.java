package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateLoanApplicationRequest(
    @NotBlank String amount,
    @NotBlank String country,
    @NotBlank String purpose,
    String contact,
    String repaymentPlan
) {
}
