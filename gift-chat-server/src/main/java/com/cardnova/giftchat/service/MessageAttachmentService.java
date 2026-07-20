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
        String normalizedType = normalizeAttachmentType(messageType, content);
        if (normalizedType.isEmpty() || !StringUtils.hasText(content)) {
            return;
        }
        if (!messageAttachmentRepository.findByOwnerMessageTypeAndOwnerMessageIdOrderBySortOrderAsc(ownerType, messageId).isEmpty()) {
            return;
        }

        create(ownerType, messageId, normalizedType, content.trim());
    }

    public void createFromUrl(String ownerType, String messageId, String attachmentType, String url) {
        String normalizedType = normalizeStoredAttachmentType(attachmentType);
        if (normalizedType.isEmpty() || !StringUtils.hasText(url)) {
            return;
        }
        if (!messageAttachmentRepository.findByOwnerMessageTypeAndOwnerMessageIdOrderBySortOrderAsc(ownerType, messageId).isEmpty()) {
            return;
        }
        create(ownerType, messageId, normalizedType, url.trim());
    }

    private void create(String ownerType, String messageId, String attachmentType, String url) {
        UploadAssetEntity asset = findUploadAsset(url);

        LocalDateTime now = LocalDateTime.now();
        MessageAttachmentEntity entity = new MessageAttachmentEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setOwnerMessageType(ownerType);
        entity.setOwnerMessageId(messageId);
        entity.setAttachmentType(attachmentType);
        entity.setAsset(asset);
        entity.setUrl(url);
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

        String normalizedType = normalizeAttachmentType(messageType, content);
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

    private String normalizeAttachmentType(String messageType, String content) {
        String normalized = messageType == null ? "" : messageType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "IMAGE" -> "IMAGE";
            case "GIF" -> "GIF";
            case "VOICE" -> "VOICE";
            case "VIDEO" -> isUploadedVideo(content) ? "VIDEO" : "CALL";
            default -> "";
        };
    }

    private boolean isUploadedVideo(String content) {
        if (!StringUtils.hasText(content)) {
            return false;
        }
        UploadAssetEntity asset = findUploadAsset(content);
        return asset != null && asset.getMimeType() != null && asset.getMimeType().startsWith("video/");
    }

    private UploadAssetEntity findUploadAsset(String url) {
        if (!StringUtils.hasText(url)) {
            return null;
        }
        String value = url.trim();
        UploadAssetEntity exact = uploadAssetRepository.findByPublicUrl(value).orElse(null);
        if (exact != null) {
            return exact;
        }

        String uploadPath = uploadPath(value);
        if (uploadPath.isEmpty()) {
            return null;
        }
        exact = uploadAssetRepository.findByPublicUrl(uploadPath).orElse(null);
        return exact != null
            ? exact
            : uploadAssetRepository.findFirstByPublicUrlEndingWith(uploadPath).orElse(null);
    }

    private String uploadPath(String value) {
        int start = value.indexOf("/uploads/");
        if (start < 0) {
            return "";
        }
        int query = value.indexOf('?', start);
        int fragment = value.indexOf('#', start);
        int end = value.length();
        if (query >= 0) {
            end = Math.min(end, query);
        }
        if (fragment >= 0) {
            end = Math.min(end, fragment);
        }
        return value.substring(start, end);
    }

    private String normalizeStoredAttachmentType(String attachmentType) {
        String normalized = attachmentType == null ? "" : attachmentType.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "IMAGE", "GIF", "VOICE", "VIDEO", "FILE", "CALL" -> normalized;
            default -> "";
        };
    }
}
