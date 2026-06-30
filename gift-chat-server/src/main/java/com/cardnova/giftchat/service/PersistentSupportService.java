package com.cardnova.giftchat.service;

import com.cardnova.giftchat.dto.SendSupportMessageRequest;
import com.cardnova.giftchat.entity.SupportConversationEntity;
import com.cardnova.giftchat.entity.SupportMessageEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.model.ChatMessage;
import com.cardnova.giftchat.model.ChatMessageSync;
import com.cardnova.giftchat.model.SupportConversation;
import com.cardnova.giftchat.repository.SupportConversationRepository;
import com.cardnova.giftchat.repository.SupportMessageRepository;
import com.cardnova.giftchat.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PersistentSupportService {

    private static final DateTimeFormatter MESSAGE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final SupportConversationRepository supportConversationRepository;
    private final SupportMessageRepository supportMessageRepository;
    private final CurrentUserService currentUserService;
    private final ConversationReadService conversationReadService;
    private final RealtimeChatService realtimeChatService;
    private final TencentMessageMirrorService tencentMessageMirrorService;
    private final MessageRateLimitService messageRateLimitService;
    private final UserPresenceService userPresenceService;
    private final UserRepository userRepository;

    public PersistentSupportService(
        SupportConversationRepository supportConversationRepository,
        SupportMessageRepository supportMessageRepository,
        CurrentUserService currentUserService,
        ConversationReadService conversationReadService,
        RealtimeChatService realtimeChatService,
        TencentMessageMirrorService tencentMessageMirrorService,
        MessageRateLimitService messageRateLimitService,
        UserPresenceService userPresenceService,
        UserRepository userRepository
    ) {
        this.supportConversationRepository = supportConversationRepository;
        this.supportMessageRepository = supportMessageRepository;
        this.currentUserService = currentUserService;
        this.conversationReadService = conversationReadService;
        this.realtimeChatService = realtimeChatService;
        this.tencentMessageMirrorService = tencentMessageMirrorService;
        this.messageRateLimitService = messageRateLimitService;
        this.userPresenceService = userPresenceService;
        this.userRepository = userRepository;
    }

    @Transactional
    public List<SupportConversation> getConversations() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        ensureUserConversation(currentUser);

        return conversationsFor(currentUser).stream()
            .map(this::toSupportConversation)
            .toList();
    }

    @Transactional
    public SupportConversation markConversationRead(String conversationId) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        SupportConversationEntity conversation = supportConversationRepository.findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Support conversation not found"));
        if (!canAccessConversation(currentUser, conversation)) {
            throw new IllegalArgumentException("Support conversation not accessible");
        }

        LocalDateTime latestMessageAt = supportMessageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId).stream()
            .map(SupportMessageEntity::getCreatedAt)
            .max(LocalDateTime::compareTo)
            .orElse(null);
        LocalDateTime readAt = conversationReadService.markRead("support", conversationId, currentUser, latestMessageAt);
        realtimeChatService.broadcastReadReceipt(
            RealtimeChatService.supportChannel(conversationId),
            "support",
            conversationId,
            currentUser.getId(),
            currentUser.getUsername(),
            MESSAGE_TIME_FORMATTER.format(readAt)
        );
        return toSupportConversation(conversation);
    }

    @Transactional
    public SupportConversation updateConversationNote(String conversationId, String note) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        currentUserService.requireAgentOrAdmin(currentUser);

        SupportConversationEntity conversation = supportConversationRepository.findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Support conversation not found"));
        if (!canAccessConversation(currentUser, conversation)) {
            throw new IllegalArgumentException("Support conversation not accessible");
        }

        String normalized = note == null ? "" : note.trim();
        if (normalized.length() > 255) {
            throw new IllegalArgumentException("Note must be 255 characters or less");
        }
        conversation.setAgentNote(normalized.isBlank() ? null : normalized);
        conversation.setUpdatedAt(LocalDateTime.now());
        return toSupportConversation(supportConversationRepository.save(conversation));
    }

    public SupportConversation toSupportConversation(SupportConversationEntity conversation) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        List<SupportMessageEntity> messages = supportMessageRepository.findByConversation_IdOrderByCreatedAtAsc(conversation.getId());
        LocalDateTime lastReadAt = conversationReadService.getLastReadAt("support", conversation.getId(), currentUser.getId());
        LocalDateTime counterpartReadAt = resolveSupportCounterpartReadAt(conversation, currentUser);
        LocalDateTime lastMessageAt = messages.stream()
            .map(SupportMessageEntity::getCreatedAt)
            .max(LocalDateTime::compareTo)
            .orElse(conversation.getUpdatedAt());
        return new SupportConversation(
            conversation.getId(),
            conversation.getCustomerUser().getUsername(),
            conversation.getAssignmentStatus(),
            conversation.getAssignedAgent() == null ? "" : conversation.getAssignedAgent().getUsername(),
            conversation.getAgentNote() == null ? "" : conversation.getAgentNote(),
            messages.stream()
                .map(message -> toChatMessage(message, currentUser, counterpartReadAt))
                .toList(),
            (int) messages.stream()
                .filter(message -> message.getSenderUser() != null)
                .filter(message -> !message.getSenderUser().getId().equals(currentUser.getId()))
                .filter(message -> lastReadAt == null || message.getCreatedAt().isAfter(lastReadAt))
                .count(),
            lastMessageAt == null ? "" : MESSAGE_TIME_FORMATTER.format(lastMessageAt),
            userPresenceService.isOnline(conversation.getCustomerUser().getId())
        );
    }

    public SupportConversationEntity getAccessibleConversationForStaff(String conversationId) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        currentUserService.requireAgentOrAdmin(currentUser);
        SupportConversationEntity conversation = supportConversationRepository.findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Support conversation not found"));
        if (!canAccessConversation(currentUser, conversation)) {
            throw new IllegalArgumentException("Support conversation not accessible");
        }
        return conversation;
    }

    public List<ChatMessage> getMessagesAfter(String conversationId, String afterId) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        SupportConversationEntity conversation = supportConversationRepository.findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Support conversation not found"));
        if (!canAccessConversation(currentUser, conversation)) {
            throw new IllegalArgumentException("Support conversation not accessible");
        }

        LocalDateTime counterpartReadAt = resolveSupportCounterpartReadAt(conversation, currentUser);
        List<SupportMessageEntity> messages = supportMessageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId);
        int startIndex = cursorStartIndex(messages.stream().map(SupportMessageEntity::getId).toList(), afterId);
        return messages.stream()
            .skip(startIndex)
            .map(message -> toChatMessage(message, currentUser, counterpartReadAt))
            .toList();
    }

    public ChatMessageSync syncMessages(String conversationId, long sinceSeq) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        SupportConversationEntity conversation = supportConversationRepository.findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Support conversation not found"));
        if (!canAccessConversation(currentUser, conversation)) {
            throw new IllegalArgumentException("Support conversation not accessible");
        }

        long normalizedSinceSeq = Math.max(0L, sinceSeq);
        LocalDateTime lastReadAt = conversationReadService.getLastReadAt("support", conversationId, currentUser.getId());
        LocalDateTime counterpartReadAt = resolveSupportCounterpartReadAt(conversation, currentUser);
        List<SupportMessageEntity> syncMessages = normalizedSinceSeq == 0L
            ? supportMessageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId)
            : supportMessageRepository.findByConversationIdSinceSeq(conversationId, normalizedSinceSeq);
        long latestSeq = supportMessageRepository.findMaxServerSeqByConversationId(conversationId);
        long readSeq = lastReadAt == null ? 0L : supportMessageRepository.findReadSeqByConversationId(conversationId, lastReadAt);
        int unreadCount = countUnread(syncMessagesForUnread(conversationId), currentUser, lastReadAt);

        return new ChatMessageSync(
            syncMessages.stream()
                .map(message -> toChatMessage(message, currentUser, counterpartReadAt))
                .toList(),
            latestSeq,
            readSeq,
            unreadCount
        );
    }

    @Transactional
    public ChatMessage sendMessage(String conversationId, SendSupportMessageRequest request) {
        UserEntity currentUser = currentUserService.getCurrentUser();

        SupportConversationEntity conversation = supportConversationRepository.findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Support conversation not found"));

        if (!canAccessConversation(currentUser, conversation)) {
            throw new IllegalArgumentException("Support conversation not accessible");
        }
        messageRateLimitService.checkSendAllowed(currentUser.getId());

        String clientMessageId = normalizeClientMessageId(request.clientMessageId());
        if (!clientMessageId.isEmpty()) {
            SupportMessageEntity existing = supportMessageRepository
                .findBySenderUser_IdAndClientMessageId(currentUser.getId(), clientMessageId)
                .orElse(null);
            if (existing != null) {
                return normalizeOwnSupportMessage(existing);
            }
        }

        SupportMessageEntity saved = appendMessageEntity(
            conversation,
            currentUser,
            isAgent(currentUser) ? "SUPPORT" : "ME",
            request.messageType(),
            request.content(),
            clientMessageId
        );

        ChatMessage message = normalizeOwnSupportMessage(saved);
        mirrorAfterCommit(saved);
        realtimeChatService.broadcast(
            RealtimeChatService.supportChannel(conversationId),
            currentUser.getId(),
            "me",
            isAgent(currentUser) ? "support" : "friend",
            saved.getMessageType().toLowerCase(),
            saved.getContent(),
            saved.getId(),
            MESSAGE_TIME_FORMATTER.format(saved.getCreatedAt()),
            saved.getClientMessageId(),
            saved.getServerSeq(),
            saved.getDeliveryStatus(),
            saved.getDeliveredAt() == null ? "" : MESSAGE_TIME_FORMATTER.format(saved.getDeliveredAt()),
            saved.getFailedReason()
        );
        return message;
    }

    @Transactional
    public ChatMessage appendStaffMessage(
        SupportConversationEntity conversation,
        UserEntity sender,
        String senderRole,
        String messageType,
        String content
    ) {
        String normalizedRole = senderRole == null ? "SUPPORT" : senderRole.trim().toUpperCase();
        SupportMessageEntity saved = appendMessageEntity(conversation, sender, normalizedRole, messageType, content, "");
        String author = "ADMIN".equals(normalizedRole) ? "support" : normalizedRole.toLowerCase();

        realtimeChatService.broadcast(
            RealtimeChatService.supportChannel(conversation.getId()),
            sender == null ? null : sender.getId(),
            "me",
            author,
            saved.getMessageType().toLowerCase(),
            saved.getContent(),
            saved.getId(),
            MESSAGE_TIME_FORMATTER.format(saved.getCreatedAt()),
            saved.getClientMessageId(),
            saved.getServerSeq(),
            saved.getDeliveryStatus(),
            saved.getDeliveredAt() == null ? "" : MESSAGE_TIME_FORMATTER.format(saved.getDeliveredAt()),
            saved.getFailedReason()
        );

        return new ChatMessage(
            saved.getId(),
            author,
            saved.getMessageType().toLowerCase(),
            saved.getContent(),
            MESSAGE_TIME_FORMATTER.format(saved.getCreatedAt()),
            "none"
        );
    }

    private List<SupportConversationEntity> conversationsFor(UserEntity user) {
        if (isAgent(user)) {
            return supportConversationRepository.findByAssignedAgent_IdOrderByUpdatedAtDesc(user.getId());
        }
        return supportConversationRepository.findByCustomerUser_IdOrderByUpdatedAtDesc(user.getId());
    }

    @Transactional
    public SupportConversationEntity ensureUserConversation(UserEntity user) {
        if (isAgent(user)) {
            return null;
        }
        SupportConversationEntity existing = supportConversationRepository.findFirstByCustomerUser_IdOrderByUpdatedAtDesc(user.getId())
            .orElse(null);
        if (existing != null) {
            if (existing.getAssignedAgent() == null) {
                existing.setAssignedAgent(selectBalancedAgent());
                existing.setAssignmentStatus("AUTO_ASSIGNED");
                existing.setUpdatedAt(LocalDateTime.now());
                return supportConversationRepository.save(existing);
            }
            return existing;
        }

        SupportConversationEntity conversation = new SupportConversationEntity();
        conversation.setId(UUID.randomUUID().toString());
        conversation.setCustomerUser(user);
        conversation.setAssignedAgent(selectBalancedAgent());
        conversation.setAssignmentStatus("AUTO_ASSIGNED");
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());
        return supportConversationRepository.save(conversation);
    }

    @Transactional
    public void appendSystemMessage(SupportConversationEntity conversation, String content) {
        SupportMessageEntity saved = appendMessageEntity(conversation, null, "SYSTEM", "TEXT", content, "");

        realtimeChatService.broadcast(
            RealtimeChatService.supportChannel(conversation.getId()),
            null,
            "system",
            "system",
            "text",
            saved.getContent(),
            saved.getId(),
            MESSAGE_TIME_FORMATTER.format(saved.getCreatedAt()),
            saved.getClientMessageId(),
            saved.getServerSeq(),
            saved.getDeliveryStatus(),
            saved.getDeliveredAt() == null ? "" : MESSAGE_TIME_FORMATTER.format(saved.getDeliveredAt()),
            saved.getFailedReason()
        );
    }

    private SupportMessageEntity appendMessageEntity(
        SupportConversationEntity conversation,
        UserEntity sender,
        String senderRole,
        String messageType,
        String content,
        String clientMessageId
    ) {
        LocalDateTime now = LocalDateTime.now();
        SupportMessageEntity entity = new SupportMessageEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setConversation(conversation);
        entity.setSenderUser(sender);
        entity.setSenderRole(senderRole.trim().toUpperCase());
        entity.setMessageType(messageType.trim().toUpperCase());
        entity.setContent(content.trim());
        String normalizedClientMessageId = normalizeClientMessageId(clientMessageId);
        entity.setClientMessageId(normalizedClientMessageId.isEmpty() ? null : normalizedClientMessageId);
        entity.setServerSeq(supportMessageRepository.findMaxServerSeqByConversationId(conversation.getId()) + 1);
        entity.setDeliveryStatus("DELIVERED");
        entity.setDeliveredAt(now);
        entity.setFailedReason("");
        entity.setTencentMirrorStatus("SKIPPED");
        entity.setTencentMessageKey("");
        entity.setTencentMirrorError("");
        entity.setCreatedAt(now);
        SupportMessageEntity saved = supportMessageRepository.save(entity);

        conversation.setUpdatedAt(now);
        supportConversationRepository.save(conversation);
        return saved;
    }

    private boolean canAccessConversation(UserEntity user, SupportConversationEntity conversation) {
        if ("ADMIN".equalsIgnoreCase(user.getRoleCode())) {
            return true;
        }
        if (isAgent(user)) {
            return conversation.getAssignedAgent() != null && conversation.getAssignedAgent().getId().equals(user.getId());
        }
        return conversation.getCustomerUser().getId().equals(user.getId());
    }

    private boolean isAgent(UserEntity user) {
        return "AGENT".equalsIgnoreCase(user.getRoleCode());
    }

    private String resolveAuthor(SupportMessageEntity message, UserEntity currentUser) {
        if (message.getSenderUser() == null) {
            return "system";
        }
        if (message.getSenderUser().getId().equals(currentUser.getId())) {
            return "me";
        }
        return "support";
    }

    private String resolveReadState(UserEntity senderUser, UserEntity currentUser, LocalDateTime createdAt, LocalDateTime counterpartReadAt) {
        if (senderUser == null || !senderUser.getId().equals(currentUser.getId())) {
            return "none";
        }
        if (counterpartReadAt != null && !createdAt.isAfter(counterpartReadAt)) {
            return "read";
        }
        return "sent";
    }

    private LocalDateTime resolveSupportCounterpartReadAt(SupportConversationEntity conversation, UserEntity currentUser) {
        UserEntity counterpart = null;
        if (conversation.getCustomerUser().getId().equals(currentUser.getId())) {
            counterpart = conversation.getAssignedAgent();
        } else {
            counterpart = conversation.getCustomerUser();
        }
        if (counterpart == null) {
            return null;
        }
        return conversationReadService.getReadAt("support", conversation.getId(), counterpart.getId());
    }

    private ChatMessage toChatMessage(SupportMessageEntity message, UserEntity currentUser, LocalDateTime counterpartReadAt) {
        return new ChatMessage(
            message.getId(),
            resolveAuthor(message, currentUser),
            message.getMessageType().toLowerCase(),
            message.getContent(),
            MESSAGE_TIME_FORMATTER.format(message.getCreatedAt()),
            resolveReadState(message.getSenderUser(), currentUser, message.getCreatedAt(), counterpartReadAt),
            message.getClientMessageId() == null ? "" : message.getClientMessageId(),
            message.getServerSeq() == null ? 0L : message.getServerSeq(),
            normalizeDeliveryStatus(message.getDeliveryStatus()),
            message.getDeliveredAt() == null ? "" : MESSAGE_TIME_FORMATTER.format(message.getDeliveredAt()),
            message.getFailedReason() == null ? "" : message.getFailedReason()
        );
    }

    private ChatMessage normalizeOwnSupportMessage(SupportMessageEntity saved) {
        return new ChatMessage(
            saved.getId(),
            "me",
            saved.getMessageType().toLowerCase(),
            saved.getContent(),
            MESSAGE_TIME_FORMATTER.format(saved.getCreatedAt()),
            "sent",
            saved.getClientMessageId() == null ? "" : saved.getClientMessageId(),
            saved.getServerSeq() == null ? 0L : saved.getServerSeq(),
            normalizeDeliveryStatus(saved.getDeliveryStatus()),
            saved.getDeliveredAt() == null ? "" : MESSAGE_TIME_FORMATTER.format(saved.getDeliveredAt()),
            saved.getFailedReason() == null ? "" : saved.getFailedReason()
        );
    }

    private String normalizeClientMessageId(String clientMessageId) {
        String normalized = clientMessageId == null ? "" : clientMessageId.trim();
        if (normalized.length() > 64) {
            throw new IllegalArgumentException("clientMessageId must be 64 characters or less");
        }
        return normalized;
    }

    private String normalizeDeliveryStatus(String deliveryStatus) {
        return deliveryStatus == null || deliveryStatus.isBlank() ? "delivered" : deliveryStatus.toLowerCase();
    }

    private List<SupportMessageEntity> syncMessagesForUnread(String conversationId) {
        return supportMessageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId);
    }

    private int countUnread(List<SupportMessageEntity> messages, UserEntity currentUser, LocalDateTime lastReadAt) {
        return (int) messages.stream()
            .filter(message -> message.getSenderUser() != null)
            .filter(message -> !message.getSenderUser().getId().equals(currentUser.getId()))
            .filter(message -> lastReadAt == null || message.getCreatedAt().isAfter(lastReadAt))
            .count();
    }

    private void mirrorAfterCommit(SupportMessageEntity message) {
        if (message.getSenderUser() == null) {
            return;
        }
        message.setTencentMirrorStatus("PENDING");
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            tencentMessageMirrorService.mirrorSupportMessage(message.getId());
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                tencentMessageMirrorService.mirrorSupportMessage(message.getId());
            }
        });
    }

    private int cursorStartIndex(List<String> messageIds, String afterId) {
        String normalized = afterId == null ? "" : afterId.trim();
        if (normalized.isEmpty()) {
            return 0;
        }
        int index = messageIds.indexOf(normalized);
        return index < 0 ? 0 : index + 1;
    }

    private UserEntity selectBalancedAgent() {
        return userRepository.findByRoleCodeAndStatusCodeOrderByCreatedAtAsc("AGENT", "ACTIVE").stream()
            .min(Comparator
                .comparingLong((UserEntity agent) -> supportConversationRepository.countByAssignedAgent_IdAndCustomerUser_StatusCode(agent.getId(), "ACTIVE"))
                .thenComparing(UserEntity::getCreatedAt))
            .orElse(null);
    }
}
