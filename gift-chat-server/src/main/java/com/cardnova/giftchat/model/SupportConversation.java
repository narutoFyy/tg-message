package com.cardnova.giftchat.model;

import java.util.List;

public record SupportConversation(
    String conversationId,
    String customerUsername,
    String assignmentStatus,
    String assignedAgent,
    String agentNote,
    List<ChatMessage> messages,
    int unreadCount,
    String lastMessageTime,
    boolean online
) {
}
