package com.cardnova.giftchat.service;

import com.cardnova.giftchat.entity.NotificationEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.model.NotificationItem;
import com.cardnova.giftchat.repository.NotificationRepository;
import com.cardnova.giftchat.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class NotificationService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public NotificationService(
        NotificationRepository notificationRepository,
        UserRepository userRepository,
        CurrentUserService currentUserService
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    public List<NotificationItem> myNotifications() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        return notificationRepository.findByRecipientUser_IdOrderByCreatedAtDesc(currentUser.getId()).stream()
            .map(this::toItem)
            .toList();
    }

    public long unreadCount() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        return notificationRepository.countByRecipientUser_IdAndReadFlagFalse(currentUser.getId());
    }

    @Transactional
    public void notifyUser(
        UserEntity recipient,
        UserEntity actor,
        String eventType,
        String title,
        String body,
        String targetType,
        String targetId
    ) {
        NotificationEntity entity = new NotificationEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setRecipientUser(recipient);
        entity.setActorUser(actor);
        entity.setEventType(eventType);
        entity.setTitle(title);
        entity.setBody(body);
        entity.setTargetType(targetType);
        entity.setTargetId(targetId);
        entity.setReadFlag(false);
        entity.setCreatedAt(LocalDateTime.now());
        notificationRepository.save(entity);
    }

    @Transactional
    public void notifyAdmins(UserEntity actor, String eventType, String title, String body, String targetType, String targetId) {
        userRepository.findByRoleCodeAndStatusCodeOrderByCreatedAtAsc("ADMIN", "ACTIVE")
            .forEach(admin -> notifyUser(admin, actor, eventType, title, body, targetType, targetId));
    }

    private NotificationItem toItem(NotificationEntity entity) {
        return new NotificationItem(
            entity.getId(),
            entity.getEventType().toLowerCase(),
            entity.getTitle(),
            entity.getBody(),
            entity.getTargetType().toLowerCase(),
            entity.getTargetId(),
            entity.isReadFlag(),
            TIME_FORMATTER.format(entity.getCreatedAt())
        );
    }
}
