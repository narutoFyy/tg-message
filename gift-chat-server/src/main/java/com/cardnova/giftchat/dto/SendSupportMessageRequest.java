package com.cardnova.giftchat.dto;

import com.cardnova.giftchat.model.ChatMessageReply;
import jakarta.validation.constraints.NotBlank;

public record SendSupportMessageRequest(
    @NotBlank String content,
    @NotBlank String messageType,
    String clientMessageId,
    ChatMessageReply replyTo
) {
}
