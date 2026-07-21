package com.cardnova.giftchat.model;

public record ChatMessage(
    String id,
    String author,
    String type,
    String content,
    String createdAt,
    String readState,
    String clientMessageId,
    Long serverSeq,
    String deliveryStatus,
    String deliveredAt,
    String failedReason,
    java.util.List<MessageAttachment> attachments,
    ChatMessageReply replyTo,
    ChatOrderItem order
) {
    public ChatMessage(
        String id,
        String author,
        String type,
        String content,
        String createdAt,
        String readState,
        String clientMessageId,
        Long serverSeq,
        String deliveryStatus,
        String deliveredAt,
        String failedReason,
        java.util.List<MessageAttachment> attachments,
        ChatMessageReply replyTo
    ) {
        this(id, author, type, content, createdAt, readState, clientMessageId, serverSeq, deliveryStatus, deliveredAt, failedReason, attachments, replyTo, null);
    }

    public ChatMessage(
        String id,
        String author,
        String type,
        String content,
        String createdAt,
        String readState,
        String clientMessageId,
        Long serverSeq,
        String deliveryStatus,
        String deliveredAt,
        String failedReason,
        java.util.List<MessageAttachment> attachments
    ) {
        this(id, author, type, content, createdAt, readState, clientMessageId, serverSeq, deliveryStatus, deliveredAt, failedReason, attachments, null, null);
    }

    public ChatMessage(
        String id,
        String author,
        String type,
        String content,
        String createdAt,
        String readState
    ) {
        this(id, author, type, content, createdAt, readState, "", 0L, "delivered", "", "", java.util.List.of(), null, null);
    }
}
