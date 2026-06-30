package com.cardnova.giftchat.service;

import com.cardnova.giftchat.dto.RegisterPushDeviceRequest;
import com.cardnova.giftchat.entity.PushDeviceEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.model.PushDeviceItem;
import com.cardnova.giftchat.repository.PushDeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;

@Service
public class PushDeviceService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final PushDeviceRepository pushDeviceRepository;
    private final CurrentUserService currentUserService;

    public PushDeviceService(PushDeviceRepository pushDeviceRepository, CurrentUserService currentUserService) {
        this.pushDeviceRepository = pushDeviceRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public PushDeviceItem register(RegisterPushDeviceRequest request) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        String provider = normalizeCode(request.provider());
        String platform = normalizeCode(request.platform());
        String token = request.deviceToken().trim();
        LocalDateTime now = LocalDateTime.now();
        PushDeviceEntity device = pushDeviceRepository.findByProviderAndDeviceToken(provider, token)
            .orElseGet(() -> {
                PushDeviceEntity created = new PushDeviceEntity();
                created.setId(deviceId(provider, token));
                created.setCreatedAt(now);
                return created;
            });
        device.setUser(currentUser);
        device.setProvider(provider);
        device.setPlatform(platform);
        device.setDeviceToken(token);
        device.setDeviceModel(normalizeOptional(request.deviceModel(), 128));
        device.setAppVersion(normalizeOptional(request.appVersion(), 32));
        device.setEnabled(true);
        device.setLastSeenAt(now);
        device.setUpdatedAt(now);
        return toItem(pushDeviceRepository.save(device));
    }

    public List<PushDeviceItem> myDevices() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        return pushDeviceRepository.findByUser_IdAndEnabledTrue(currentUser.getId()).stream()
            .map(this::toItem)
            .toList();
    }

    @Transactional
    public PushDeviceItem disable(String deviceId) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        PushDeviceEntity device = pushDeviceRepository.findById(deviceId)
            .orElseThrow(() -> new IllegalArgumentException("Push device not found"));
        if (!device.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Push device not accessible");
        }
        device.setEnabled(false);
        device.setUpdatedAt(LocalDateTime.now());
        return toItem(pushDeviceRepository.save(device));
    }

    private PushDeviceItem toItem(PushDeviceEntity device) {
        return new PushDeviceItem(
            device.getId(),
            device.getPlatform().toLowerCase(),
            device.getProvider().toLowerCase(),
            device.getDeviceModel() == null ? "" : device.getDeviceModel(),
            device.getAppVersion() == null ? "" : device.getAppVersion(),
            device.isEnabled(),
            TIME_FORMATTER.format(device.getLastSeenAt())
        );
    }

    private String normalizeCode(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private String normalizeOptional(String value, int maxLength) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > maxLength) {
            return normalized.substring(0, maxLength);
        }
        return normalized;
    }

    private String deviceId(String provider, String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((provider + ":" + token).getBytes(StandardCharsets.UTF_8));
            return "pd_" + HexFormat.of().formatHex(hash).substring(0, 32);
        } catch (Exception exception) {
            throw new IllegalStateException("Push device id generation failed", exception);
        }
    }
}
