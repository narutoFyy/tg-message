package com.cardnova.giftchat.service;

import com.cardnova.giftchat.dto.CreateRateRequest;
import com.cardnova.giftchat.entity.GiftCardRateEntity;
import com.cardnova.giftchat.model.RateItem;
import com.cardnova.giftchat.repository.GiftCardRateRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class PersistentRateService {

    private static final DateTimeFormatter RATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Set<String> ALLOWED_STATUSES = Set.of("active", "paused");

    private final GiftCardRateRepository giftCardRateRepository;
    private final CurrentUserService currentUserService;

    public PersistentRateService(GiftCardRateRepository giftCardRateRepository, CurrentUserService currentUserService) {
        this.giftCardRateRepository = giftCardRateRepository;
        this.currentUserService = currentUserService;
    }

    public List<RateItem> findAll() {
        return giftCardRateRepository.findAllByOrderByUpdatedAtDesc().stream()
            .map(this::toRateItem)
            .toList();
    }

    public RateItem create(CreateRateRequest request) {
        var currentUser = currentUserService.getCurrentUser();
        currentUserService.requireAdmin(currentUser);

        GiftCardRateEntity entity = new GiftCardRateEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setCardName(request.cardName().trim());
        entity.setRegionCode(request.region().trim());
        entity.setRateValue(request.rate().trim());
        entity.setStatusCode("ACTIVE");
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(currentUser);
        return toRateItem(giftCardRateRepository.save(entity));
    }

    public RateItem updateStatus(String rateId, String status) {
        var currentUser = currentUserService.getCurrentUser();
        currentUserService.requireAdmin(currentUser);

        String normalizedStatus = status == null ? "" : status.trim().toLowerCase();
        if (!ALLOWED_STATUSES.contains(normalizedStatus)) {
            throw new IllegalArgumentException("Unsupported rate status");
        }

        GiftCardRateEntity entity = giftCardRateRepository.findById(rateId)
            .orElseThrow(() -> new IllegalArgumentException("Rate not found"));
        entity.setStatusCode(normalizedStatus.toUpperCase());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(currentUser);
        return toRateItem(giftCardRateRepository.save(entity));
    }

    public RateItem update(String rateId, CreateRateRequest request) {
        var currentUser = currentUserService.getCurrentUser();
        currentUserService.requireAdmin(currentUser);

        GiftCardRateEntity entity = giftCardRateRepository.findById(rateId)
            .orElseThrow(() -> new IllegalArgumentException("Rate not found"));
        entity.setCardName(request.cardName().trim());
        entity.setRegionCode(request.region().trim());
        entity.setRateValue(request.rate().trim());
        entity.setUpdatedAt(LocalDateTime.now());
        entity.setUpdatedBy(currentUser);
        return toRateItem(giftCardRateRepository.save(entity));
    }

    public RateItem delete(String rateId) {
        var currentUser = currentUserService.getCurrentUser();
        currentUserService.requireAdmin(currentUser);

        GiftCardRateEntity entity = giftCardRateRepository.findById(rateId)
            .orElseThrow(() -> new IllegalArgumentException("Rate not found"));
        RateItem deleted = toRateItem(entity);
        giftCardRateRepository.delete(entity);
        return deleted;
    }

    private RateItem toRateItem(GiftCardRateEntity entity) {
        return new RateItem(
            entity.getId(),
            entity.getCardName(),
            entity.getRegionCode(),
            entity.getRateValue(),
            entity.getStatusCode().equalsIgnoreCase("ACTIVE") ? "active" : "paused",
            RATE_TIME_FORMATTER.format(entity.getUpdatedAt())
        );
    }
}
