package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateBroadcastRequest(
    @NotBlank String scope,
    @NotBlank String content,
    @NotBlank String messageType,
    List<String> countryCodes,
    String keyword,
    List<String> targetConversationIds
) {
}
