package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record HideRecordRequest(
    @NotBlank @Size(max = 32) String targetType,
    @NotBlank @Size(max = 64) String targetId,
    @Size(max = 32) String hiddenScope
) {
}
