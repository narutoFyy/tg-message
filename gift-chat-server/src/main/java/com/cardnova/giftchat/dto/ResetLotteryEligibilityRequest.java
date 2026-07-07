package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetLotteryEligibilityRequest(
    @NotBlank @Size(max = 255) String reason
) {
}
