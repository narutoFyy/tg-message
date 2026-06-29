package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignSupportConversationRequest(
    @NotBlank String agentUsername
) {
}
