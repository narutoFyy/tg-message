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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
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

        String detectedMimeType = detectMimeType(file);
        if (!allowedTypes.contains(detectedMimeType)) {
            throw new IllegalArgumentException("Unsupported " + label.toLowerCase() + " type");
        }
        try {
            Files.createDirectories(storageDir);
            String extension = extensionForMimeType(detectedMimeType, extensionFromName(file.getOriginalFilename()));
            String id = UUID.randomUUID().toString();
            String storedName = id + (extension.isEmpty() ? "" : "." + extension);
            Path destination = storageDir.resolve(storedName);
            Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);

            UploadAssetEntity entity = new UploadAssetEntity();
            entity.setId(id);
            entity.setOwnerUser(currentUserService.getCurrentUser());
            entity.setOriginalName(StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename().trim() : storedName);
            entity.setMimeType(detectedMimeType);
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

    private String detectMimeType(MultipartFile file) {
        byte[] header = new byte[16];
        int readBytes;
        try (InputStream inputStream = file.getInputStream()) {
            readBytes = inputStream.read(header);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to inspect upload file", exception);
        }
        if (readBytes < 0) {
            readBytes = 0;
        }

        String detected = detectFromHeader(header, readBytes);
        if (StringUtils.hasText(detected)) {
            return detected;
        }

        return "application/octet-stream";
    }

    private String detectFromHeader(byte[] header, int length) {
        if (length >= 8
            && unsigned(header[0]) == 0x89
            && header[1] == 0x50
            && header[2] == 0x4E
            && header[3] == 0x47
            && header[4] == 0x0D
            && header[5] == 0x0A
            && header[6] == 0x1A
            && header[7] == 0x0A) {
            return "image/png";
        }
        if (length >= 3 && unsigned(header[0]) == 0xFF && unsigned(header[1]) == 0xD8 && unsigned(header[2]) == 0xFF) {
            return "image/jpeg";
        }
        if (length >= 6 && ascii(header, 0, 6).matches("GIF8[79]a")) {
            return "image/gif";
        }
        if (length >= 12 && "RIFF".equals(ascii(header, 0, 4)) && "WEBP".equals(ascii(header, 8, 4))) {
            return "image/webp";
        }
        if (length >= 3 && "ID3".equals(ascii(header, 0, 3))) {
            return "audio/mpeg";
        }
        if (length >= 2 && unsigned(header[0]) == 0xFF && (unsigned(header[1]) & 0xE0) == 0xE0) {
            return "audio/mpeg";
        }
        if (length >= 2 && unsigned(header[0]) == 0xFF && (unsigned(header[1]) & 0xF0) == 0xF0) {
            return "audio/aac";
        }
        if (length >= 12 && "RIFF".equals(ascii(header, 0, 4)) && "WAVE".equals(ascii(header, 8, 4))) {
            return "audio/wav";
        }
        if (length >= 12 && "ftyp".equals(ascii(header, 4, 4))) {
            return "audio/mp4";
        }
        if (length >= 4 && "#!AM".equals(ascii(header, 0, 4))) {
            return "audio/amr";
        }
        if (length >= 4 && unsigned(header[0]) == 0x1A && header[1] == 0x45 && unsigned(header[2]) == 0xDF && unsigned(header[3]) == 0xA3) {
            return "audio/webm";
        }
        return "";
    }

    private String ascii(byte[] bytes, int offset, int length) {
        if (bytes.length < offset + length) {
            return "";
        }
        return new String(bytes, offset, length, java.nio.charset.StandardCharsets.US_ASCII);
    }

    private int unsigned(byte value) {
        return value & 0xFF;
    }

    private String extensionForMimeType(String mimeType, String fallback) {
        return switch (mimeType.toLowerCase(Locale.ROOT)) {
            case "image/png" -> "png";
            case "image/jpeg" -> "jpg";
            case "image/gif" -> "gif";
            case "image/webp" -> "webp";
            case "audio/mpeg", "audio/mp3" -> "mp3";
            case "audio/mp4", "audio/aac" -> "m4a";
            case "audio/wav", "audio/x-wav" -> "wav";
            case "audio/webm" -> "webm";
            case "audio/amr" -> "amr";
            default -> fallback;
        };
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
        return originalName.substring(originalName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }
}
