package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotNull;

public record UpdatePromotionInviteCodeStatusRequest(
    @NotNull Boolean enabled
) {
}
