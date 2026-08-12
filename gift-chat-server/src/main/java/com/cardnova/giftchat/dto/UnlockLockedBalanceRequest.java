package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.Size;

public record UnlockLockedBalanceRequest(
    @Size(max = 255) String reason
) {
}
