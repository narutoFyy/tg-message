package com.cardnova.giftchat.service;

import com.cardnova.giftchat.entity.MessageAttachmentEntity;
import com.cardnova.giftchat.entity.UploadAssetEntity;
import com.cardnova.giftchat.model.MessageAttachment;
import com.cardnova.giftchat.repository.MessageAttachmentRepository;
import com.cardnova.giftchat.repository.UploadAssetRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class MessageAttachmentService {

    private final MessageAttachmentRepository messageAttachmentRepository;
    private final UploadAssetRepository uploadAssetRepository;

    public MessageAttachmentService(
        MessageAttachmentRepository messageAttachmentRepository,
        UploadAssetRepository uploadAssetRepository
    ) {
        this.messageAttachmentRepository = messageAttachmentRepository;
        this.uploadAssetRepository = uploadAssetRepository;
    }

    public void createFromMessageContent(String ownerType, String messageId, String messageType, String content) {
        String normalizedType = normalizeAttachmentType(messageType);
        if (normalizedType.isEmpty() || !StringUtils.hasText(content)) {
            return;
        }
        if (!messageAttachmentRepository.findByOwnerMessageTypeAndOwnerMessageIdOrderBySortOrderAsc(ownerType, messageId).isEmpty()) {
            return;
        }

        UploadAssetEntity asset = uploadAssetRepository.findByPublicUrl(content.trim()).orElse(null);

        LocalDateTime now = LocalDateTime.now();
        MessageAttachmentEntity entity = new MessageAttachmentEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setOwnerMessageType(ownerType);
        entity.setOwnerMessageId(messageId);
        entity.setAttachmentType(normalizedType);
        entity.setAsset(asset);
        entity.setUrl(content.trim());
        entity.setThumbnailUrl("");
        entity.setMimeType(asset == null ? "" : asset.getMimeType());
        entity.setOriginalName(asset == null ? "" : asset.getOriginalName());
        entity.setSizeBytes(asset == null ? 0L : asset.getSizeBytes());
        entity.setSortOrder(0);
        entity.setStatusCode("READY");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        messageAttachmentRepository.save(entity);
    }

    public List<MessageAttachment> attachmentsFor(String ownerType, String messageId, String messageType, String content) {
        List<MessageAttachment> attachments = messageAttachmentRepository
            .findByOwnerMessageTypeAndOwnerMessageIdOrderBySortOrderAsc(ownerType, messageId)
            .stream()
            .map(this::toModel)
            .toList();
        if (!attachments.isEmpty()) {
            return attachments;
        }

        String normalizedType = normalizeAttachmentType(messageType);
        if (normalizedType.isEmpty() || !StringUtils.hasText(content)) {
            return List.of();
        }
        return List.of(new MessageAttachment(
            "",
            normalizedType.toLowerCase(Locale.ROOT),
            content.trim(),
            "",
            "",
            "",
            0L,
            0,
            0,
            0L,
            "ready"
        ));
    }

    private MessageAttachment toModel(MessageAttachmentEntity entity) {
        return new MessageAttachment(
            entity.getId(),
            entity.getAttachmentType().toLowerCase(Locale.ROOT),
            entity.getUrl(),
            entity.getThumbnailUrl() == null ? "" : entity.getThumbnailUrl(),
            entity.getMimeType() == null ? "" : entity.getMimeType(),
            entity.getOriginalName() == null ? "" : entity.getOriginalName(),
            entity.getSizeBytes() == null ? 0L : entity.getSizeBytes(),
            entity.getWidth() == null ? 0 : entity.getWidth(),
            entity.getHeight() == null ? 0 : entity.getHeight(),
            entity.getDurationMs() == null ? 0L : entity.getDurationMs(),
            entity.getStatusCode() == null ? "ready" : entity.getStatusCode().toLowerCase(Locale.ROOT)
        );
    }

    private String normalizeAttachmentType(String messageType) {
        String normalized = messageType == null ? "" : messageType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "IMAGE" -> "IMAGE";
            case "GIF" -> "GIF";
            case "VOICE" -> "VOICE";
            case "VIDEO" -> "CALL";
            default -> "";
        };
    }
}
