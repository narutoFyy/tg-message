package com.cardnova.giftchat.model;

public record MessageAttachment(
    String id,
    String type,
    String url,
    String thumbnailUrl,
    String mimeType,
    String originalName,
    Long sizeBytes,
    Integer width,
    Integer height,
    Long durationMs,
    String status
) {
}
