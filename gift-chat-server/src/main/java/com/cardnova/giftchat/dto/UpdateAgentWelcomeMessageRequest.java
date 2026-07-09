package com.cardnova.giftchat.dto;

public record UpdateAgentWelcomeMessageRequest(
    String content,
    Boolean enabled
) {
}
