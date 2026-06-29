package com.cardnova.giftchat.model;

import java.util.List;

public record AdminDirectConversation(
    String friendshipId,
    String requesterUsername,
    String addresseeUsername,
    String status,
    List<ChatMessage> messages
) {
}
