package com.cardnova.giftchat.service;

import com.cardnova.giftchat.dto.CreateBroadcastRequest;
import com.cardnova.giftchat.entity.BroadcastMessageEntity;
import com.cardnova.giftchat.entity.SupportConversationEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.model.BroadcastItem;
import com.cardnova.giftchat.repository.BroadcastMessageRepository;
import com.cardnova.giftchat.repository.SupportConversationRepository;
import com.cardnova.giftchat.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class BroadcastService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BroadcastMessageRepository broadcastMessageRepository;
    private final SupportConversationRepository supportConversationRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final PersistentSupportService persistentSupportService;
    private final NotificationService notificationService;

    public BroadcastService(
        BroadcastMessageRepository broadcastMessageRepository,
        SupportConversationRepository supportConversationRepository,
        UserRepository userRepository,
        CurrentUserService currentUserService,
        PersistentSupportService persistentSupportService,
        NotificationService notificationService
    ) {
        this.broadcastMessageRepository = broadcastMessageRepository;
        this.supportConversationRepository = supportConversationRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.persistentSupportService = persistentSupportService;
        this.notificationService = notificationService;
    }

    public List<BroadcastItem> getBroadcasts() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        currentUserService.requireAgentOrAdmin(currentUser);

        if ("ADMIN".equalsIgnoreCase(currentUser.getRoleCode())) {
            return broadcastMessageRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toItem)
                .toList();
        }

        return broadcastMessageRepository.findBySenderUser_IdOrderByCreatedAtDesc(currentUser.getId()).stream()
            .map(this::toItem)
            .toList();
    }

    @Transactional
    public BroadcastItem create(CreateBroadcastRequest request) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        currentUserService.requireAgentOrAdmin(currentUser);

        String scope = normalizeScope(currentUser, request.scope());
        String messageType = normalizeMessageType(request.messageType());
        String content = request.content().trim();
        List<SupportConversationEntity> targets = resolveTargets(currentUser, scope);

        String senderRole = "ADMIN".equalsIgnoreCase(currentUser.getRoleCode()) ? "ADMIN" : "SUPPORT";
        targets.forEach(conversation -> {
            persistentSupportService.appendStaffMessage(conversation, currentUser, senderRole, messageType, content);
            notificationService.notifyUser(
                conversation.getCustomerUser(),
                currentUser,
                "BROADCAST",
                "New message",
                currentUser.getUsername() + " sent a broadcast message.",
                "SUPPORT_CONVERSATION",
                conversation.getId()
            );
        });

        BroadcastMessageEntity entity = new BroadcastMessageEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setSenderUser(currentUser);
        entity.setSenderRole(currentUser.getRoleCode().toUpperCase());
        entity.setScopeCode(scope.toUpperCase());
        entity.setMessageType(messageType.toUpperCase());
        entity.setContent(content);
        entity.setDeliveredCount(targets.size());
        entity.setCreatedAt(LocalDateTime.now());
        return toItem(broadcastMessageRepository.save(entity));
    }

    private List<SupportConversationEntity> resolveTargets(UserEntity currentUser, String scope) {
        if ("own".equals(scope)) {
            return supportConversationRepository.findByAssignedAgent_IdOrderByUpdatedAtDesc(currentUser.getId()).stream()
                .filter(conversation -> "ACTIVE".equalsIgnoreCase(conversation.getCustomerUser().getStatusCode()))
                .toList();
        }

        return userRepository.findByRoleCodeAndStatusCodeOrderByCreatedAtAsc("USER", "ACTIVE").stream()
            .map(persistentSupportService::ensureUserConversation)
            .toList();
    }

    private String normalizeScope(UserEntity currentUser, String scope) {
        String normalized = scope.trim().toLowerCase();
        if ("AGENT".equalsIgnoreCase(currentUser.getRoleCode())) {
            if (!"own".equals(normalized)) {
                throw new IllegalArgumentException("Agents can only broadcast to own customers");
            }
            return normalized;
        }
        if (!"all".equals(normalized)) {
            throw new IllegalArgumentException("Admins can only broadcast to all users");
        }
        return normalized;
    }

    private String normalizeMessageType(String messageType) {
        String normalized = messageType.trim().toLowerCase();
        if (!List.of("text", "image", "voice", "link", "gif").contains(normalized)) {
            throw new IllegalArgumentException("Unsupported broadcast message type");
        }
        return normalized;
    }

    private BroadcastItem toItem(BroadcastMessageEntity entity) {
        return new BroadcastItem(
            entity.getId(),
            entity.getSenderUser().getUsername(),
            entity.getSenderRole().toLowerCase(),
            entity.getScopeCode().toLowerCase(),
            entity.getMessageType().toLowerCase(),
            entity.getContent(),
            entity.getDeliveredCount(),
            TIME_FORMATTER.format(entity.getCreatedAt())
        );
    }
}
