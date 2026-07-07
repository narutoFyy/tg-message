package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateLotteryRecordStatusRequest(
    @NotBlank @Size(max = 32) String status
) {
}
