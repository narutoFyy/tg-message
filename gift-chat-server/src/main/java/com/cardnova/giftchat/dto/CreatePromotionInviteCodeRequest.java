package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreatePromotionInviteCodeRequest(
    @NotBlank @Size(max = 32) String code
) {
}
