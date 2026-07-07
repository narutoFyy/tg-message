package com.cardnova.giftchat.service;

import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.entity.UserHiddenRecordEntity;
import com.cardnova.giftchat.model.HiddenRecordItem;
import com.cardnova.giftchat.repository.UserHiddenRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserHiddenRecordService {

    public static final String TYPE_ORDER = "ORDER";
    public static final String TYPE_MESSAGE = "MESSAGE";
    public static final String TYPE_CONVERSATION = "CONVERSATION";

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Set<String> TARGET_TYPES = Set.of(TYPE_ORDER, TYPE_MESSAGE, TYPE_CONVERSATION);

    private final UserHiddenRecordRepository userHiddenRecordRepository;
    private final CurrentUserService currentUserService;

    public UserHiddenRecordService(
        UserHiddenRecordRepository userHiddenRecordRepository,
        CurrentUserService currentUserService
    ) {
        this.userHiddenRecordRepository = userHiddenRecordRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public HiddenRecordItem hideCurrentUserRecord(String targetType, String targetId, String hiddenScope) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        String normalizedType = normalizeTargetType(targetType);
        String normalizedTargetId = requireTargetId(targetId);
        String normalizedScope = normalizeScope(hiddenScope, normalizedType);

        UserHiddenRecordEntity entity = userHiddenRecordRepository
            .findByUser_IdAndTargetTypeAndTargetIdAndHiddenScope(
                currentUser.getId(),
                normalizedType,
                normalizedTargetId,
                normalizedScope
            )
            .orElseGet(() -> {
                UserHiddenRecordEntity created = new UserHiddenRecordEntity();
                created.setId(UUID.randomUUID().toString());
                created.setUser(currentUser);
                created.setTargetType(normalizedType);
                created.setTargetId(normalizedTargetId);
                created.setHiddenScope(normalizedScope);
                return created;
            });

        entity.setCreatedAt(LocalDateTime.now());
        entity.setRestoredAt(null);
        return toItem(userHiddenRecordRepository.save(entity));
    }

    public java.util.List<HiddenRecordItem> currentUserRecords() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        return userHiddenRecordRepository.findByUser_IdAndRestoredAtIsNullOrderByCreatedAtDesc(currentUser.getId()).stream()
            .map(this::toItem)
            .toList();
    }

    public boolean isHidden(String userId, String targetType, String targetId, String hiddenScope) {
        if (!StringUtils.hasText(userId) || !StringUtils.hasText(targetId)) {
            return false;
        }
        return userHiddenRecordRepository.existsByUser_IdAndTargetTypeAndTargetIdAndHiddenScopeAndRestoredAtIsNull(
            userId,
            normalizeTargetType(targetType),
            targetId,
            normalizeScope(hiddenScope, normalizeTargetType(targetType))
        );
    }

    public Set<String> hiddenTargetIds(String userId, String targetType, String hiddenScope) {
        if (!StringUtils.hasText(userId)) {
            return Set.of();
        }
        String normalizedType = normalizeTargetType(targetType);
        String normalizedScope = normalizeScope(hiddenScope, normalizedType);
        return userHiddenRecordRepository
            .findByUser_IdAndTargetTypeAndHiddenScopeAndRestoredAtIsNull(userId, normalizedType, normalizedScope)
            .stream()
            .map(UserHiddenRecordEntity::getTargetId)
            .collect(Collectors.toSet());
    }

    public Set<String> hiddenTargetIds(String userId, String targetType, Collection<String> targetIds) {
        if (!StringUtils.hasText(userId) || targetIds == null || targetIds.isEmpty()) {
            return Set.of();
        }
        return userHiddenRecordRepository
            .findByUser_IdAndTargetTypeAndTargetIdInAndRestoredAtIsNull(userId, normalizeTargetType(targetType), targetIds)
            .stream()
            .map(UserHiddenRecordEntity::getTargetId)
            .collect(Collectors.toSet());
    }

    private HiddenRecordItem toItem(UserHiddenRecordEntity entity) {
        return new HiddenRecordItem(
            entity.getId(),
            entity.getTargetType().toLowerCase(Locale.ROOT),
            entity.getTargetId(),
            entity.getHiddenScope().toLowerCase(Locale.ROOT),
            TIME_FORMATTER.format(entity.getCreatedAt()),
            entity.getRestoredAt() == null ? "" : TIME_FORMATTER.format(entity.getRestoredAt())
        );
    }

    private String normalizeTargetType(String targetType) {
        if (!StringUtils.hasText(targetType)) {
            throw new IllegalArgumentException("Target type is required");
        }
        String normalized = targetType.trim().toUpperCase(Locale.ROOT);
        if (!TARGET_TYPES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported hidden record target type");
        }
        return normalized;
    }

    private String normalizeScope(String hiddenScope, String targetType) {
        if (StringUtils.hasText(hiddenScope)) {
            return hiddenScope.trim().toUpperCase(Locale.ROOT);
        }
        return switch (targetType) {
            case TYPE_ORDER -> "ORDER";
            case TYPE_CONVERSATION -> "CONVERSATION";
            default -> "SINGLE";
        };
    }

    private String requireTargetId(String targetId) {
        if (!StringUtils.hasText(targetId)) {
            throw new IllegalArgumentException("Target id is required");
        }
        return targetId.trim();
    }
}
