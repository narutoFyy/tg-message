package com.cardnova.giftchat.model;

import java.util.List;

public record ChatMessageSync(
    List<ChatMessage> messages,
    long latestSeq,
    long readSeq,
    int unreadCount
) {
}
