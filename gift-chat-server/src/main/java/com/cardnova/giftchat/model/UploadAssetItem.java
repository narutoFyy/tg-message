package com.cardnova.giftchat.model;

public record UploadAssetItem(
    String id,
    String originalName,
    String mimeType,
    String publicUrl,
    long sizeBytes,
    String createdAt
) {
}
