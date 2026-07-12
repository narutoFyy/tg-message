package com.cardnova.giftchat.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateBroadcastRequest(
    @NotBlank String scope,
    String content,
    @NotBlank String messageType,
    String mediaUrl,
    List<String> countryCodes,
    String keyword,
    List<String> targetConversationIds
) {
}
