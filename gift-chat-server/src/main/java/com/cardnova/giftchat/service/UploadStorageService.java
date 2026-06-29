package com.cardnova.giftchat.service;

import com.cardnova.giftchat.entity.UploadAssetEntity;
import com.cardnova.giftchat.model.UploadAssetItem;
import com.cardnova.giftchat.repository.UploadAssetRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class UploadStorageService {

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of("image/png", "image/jpeg", "image/webp", "image/gif");
    private static final Set<String> ALLOWED_VOICE_TYPES = Set.of(
        "audio/mpeg",
        "audio/mp3",
        "audio/mp4",
        "audio/aac",
        "audio/wav",
        "audio/x-wav",
        "audio/webm",
        "audio/amr"
    );
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UploadAssetRepository uploadAssetRepository;
    private final CurrentUserService currentUserService;
    private final Path imageDir;
    private final Path voiceDir;
    private final String publicBaseUrl;

    public UploadStorageService(
        UploadAssetRepository uploadAssetRepository,
        CurrentUserService currentUserService,
        @Value("${app.upload.image-dir}") String imageDir,
        @Value("${app.upload.voice-dir}") String voiceDir,
        @Value("${app.upload.public-base-url}") String publicBaseUrl
    ) {
        this.uploadAssetRepository = uploadAssetRepository;
        this.currentUserService = currentUserService;
        this.imageDir = Paths.get(imageDir).toAbsolutePath().normalize();
        this.voiceDir = Paths.get(voiceDir).toAbsolutePath().normalize();
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.trim();
    }

    public UploadAssetItem saveImage(MultipartFile file) {
        return saveAsset(file, ALLOWED_IMAGE_TYPES, "Image", imageDir, "images");
    }

    public UploadAssetItem saveVoice(MultipartFile file) {
        return saveAsset(file, ALLOWED_VOICE_TYPES, "Voice", voiceDir, "voices");
    }

    private UploadAssetItem saveAsset(
        MultipartFile file,
        Set<String> allowedTypes,
        String label,
        Path storageDir,
        String publicSegment
    ) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException(label + " file is empty");
        }

        String mimeType = file.getContentType();
        if (!allowedTypes.contains(mimeType)) {
            throw new IllegalArgumentException("Unsupported " + label.toLowerCase() + " type");
        }
        try {
            Files.createDirectories(storageDir);
            String extension = extensionFromName(file.getOriginalFilename());
            String id = UUID.randomUUID().toString();
            String storedName = id + (extension.isEmpty() ? "" : "." + extension);
            Path destination = storageDir.resolve(storedName);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            UploadAssetEntity entity = new UploadAssetEntity();
            entity.setId(id);
            entity.setOwnerUser(currentUserService.getCurrentUser());
            entity.setOriginalName(StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename().trim() : storedName);
            entity.setMimeType(mimeType);
            entity.setStoragePath(destination.toString());
            entity.setPublicUrl(resolvePublicUrl(publicSegment, storedName));
            entity.setSizeBytes(file.getSize());
            entity.setCreatedAt(LocalDateTime.now());

            UploadAssetEntity saved = uploadAssetRepository.save(entity);
            return new UploadAssetItem(
                saved.getId(),
                saved.getOriginalName(),
                saved.getMimeType(),
                saved.getPublicUrl(),
                saved.getSizeBytes(),
                TIME_FORMATTER.format(saved.getCreatedAt())
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to store " + label.toLowerCase(), exception);
        }
    }

    private String resolvePublicUrl(String publicSegment, String storedName) {
        if (StringUtils.hasText(publicBaseUrl)) {
            return publicBaseUrl.replaceAll("/+$", "") + "/uploads/" + publicSegment + "/" + storedName;
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/uploads/")
            .path(publicSegment)
            .path("/")
            .path(storedName)
            .toUriString();
    }

    private String extensionFromName(String originalName) {
        if (!StringUtils.hasText(originalName) || !originalName.contains(".")) {
            return "";
        }
        return originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase();
    }
}
