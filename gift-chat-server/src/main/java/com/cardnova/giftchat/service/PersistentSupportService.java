package com.cardnova.giftchat.service;

import com.cardnova.giftchat.dto.SendSupportMessageRequest;
import com.cardnova.giftchat.entity.AgentWelcomeMessageEntity;
import com.cardnova.giftchat.entity.SupportConversationEntity;
import com.cardnova.giftchat.entity.SupportMessageEntity;
import com.cardnova.giftchat.entity.TradeOrderEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.model.ChatMessage;
import com.cardnova.giftchat.model.ChatMessageReply;
import com.cardnova.giftchat.model.ChatOrderItem;
import com.cardnova.giftchat.model.ChatMessageSync;
import com.cardnova.giftchat.model.SupportConversation;
import com.cardnova.giftchat.repository.SupportConversationRepository;
import com.cardnova.giftchat.repository.SupportMessageRepository;
import com.cardnova.giftchat.repository.AgentWelcomeMessageRepository;
import com.cardnova.giftchat.repository.UserRepository;
import org.springframework.util.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PersistentSupportService {

    private static final DateTimeFormatter MESSAGE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final SupportConversationRepository supportConversationRepository;
    private final SupportMessageRepository supportMessageRepository;
    private final AgentWelcomeMessageRepository agentWelcomeMessageRepository;
    private final CurrentUserService currentUserService;
    private final ConversationReadService conversationReadService;
    private final RealtimeChatService realtimeChatService;
    private final TencentMessageMirrorService tencentMessageMirrorService;
    private final MessageRateLimitService messageRateLimitService;
    private final UserPresenceService userPresenceService;
    private final UserRepository userRepository;
    private final MessageAttachmentService messageAttachmentService;
    private final TranslationService translationService;
    private final PhoneCountryCodeResolver phoneCountryCodeResolver;
    private final RegistrationBonusService registrationBonusService;
    private final VipService vipService;
    private final UserHiddenRecordService userHiddenRecordService;

    public PersistentSupportService(
        SupportConversationRepository supportConversationRepository,
        SupportMessageRepository supportMessageRepository,
        AgentWelcomeMessageRepository agentWelcomeMessageRepository,
        CurrentUserService currentUserService,
        ConversationReadService conversationReadService,
        RealtimeChatService realtimeChatService,
        TencentMessageMirrorService tencentMessageMirrorService,
        MessageRateLimitService messageRateLimitService,
        UserPresenceService userPresenceService,
        UserRepository userRepository,
        MessageAttachmentService messageAttachmentService,
        TranslationService translationService,
        PhoneCountryCodeResolver phoneCountryCodeResolver,
        RegistrationBonusService registrationBonusService,
        VipService vipService,
        UserHiddenRecordService userHiddenRecordService
    ) {
        this.supportConversationRepository = supportConversationRepository;
        this.supportMessageRepository = supportMessageRepository;
        this.agentWelcomeMessageRepository = agentWelcomeMessageRepository;
        this.currentUserService = currentUserService;
        this.conversationReadService = conversationReadService;
        this.realtimeChatService = realtimeChatService;
        this.tencentMessageMirrorService = tencentMessageMirrorService;
        this.messageRateLimitService = messageRateLimitService;
        this.userPresenceService = userPresenceService;
        this.userRepository = userRepository;
        this.messageAttachmentService = messageAttachmentService;
        this.translationService = translationService;
        this.phoneCountryCodeResolver = phoneCountryCodeResolver;
        this.registrationBonusService = registrationBonusService;
        this.vipService = vipService;
        this.userHiddenRecordService = userHiddenRecordService;
    }

    @Transactional
    public List<SupportConversation> getConversations() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        if (isCustomer(currentUser)) {
            ensureUserConversation(currentUser);
        }

        return conversationsFor(currentUser).stream()
            .filter(conversation -> shouldShowConversation(conversation, currentUser))
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
        java.util.Set<String> hiddenMessageIds = hiddenMessageIds(currentUser, messages.stream().map(SupportMessageEntity::getId).toList());
        java.util.List<SupportMessageEntity> visibleMessages = messages.stream()
            .filter(message -> !hiddenMessageIds.contains(message.getId()))
            .toList();

        return new SupportConversation(
            conversation.getId(),
            conversation.getCustomerUser().getUsername(),
            conversation.getCustomerUser().getAvatarUrl() == null ? "" : conversation.getCustomerUser().getAvatarUrl(),
            conversation.getCustomerUser().getPhone() == null ? "" : conversation.getCustomerUser().getPhone(),
            phoneCountryCodeResolver.resolve(conversation.getCustomerUser().getPhone(), registrationBonusService.configuredCountryCodes()),
            vipService.summaryForUser(conversation.getCustomerUser().getId()).level(),
            vipService.summaryForUser(conversation.getCustomerUser().getId()).points(),
            conversation.getAssignmentStatus(),
            conversation.getAssignedAgent() == null ? "" : conversation.getAssignedAgent().getUsername(),
            conversation.getAgentNote() == null ? "" : conversation.getAgentNote(),
            visibleMessages.stream()
                .map(message -> toChatMessage(message, currentUser, counterpartReadAt))
                .toList(),
            (int) visibleMessages.stream()
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
        if (!shouldShowConversation(conversation, currentUser)) {
            throw new IllegalArgumentException("Support conversation not accessible");
        }

        LocalDateTime counterpartReadAt = resolveSupportCounterpartReadAt(conversation, currentUser);
        List<SupportMessageEntity> messages = supportMessageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId);
        int startIndex = cursorStartIndex(messages.stream().map(SupportMessageEntity::getId).toList(), afterId);
        java.util.Set<String> hiddenMessageIds = hiddenMessageIds(currentUser, messages.stream().map(SupportMessageEntity::getId).toList());
        return messages.stream()
            .skip(startIndex)
            .filter(message -> !hiddenMessageIds.contains(message.getId()))
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
        if (!shouldShowConversation(conversation, currentUser)) {
            throw new IllegalArgumentException("Support conversation not accessible");
        }

        long normalizedSinceSeq = Math.max(0L, sinceSeq);
        LocalDateTime lastReadAt = conversationReadService.getLastReadAt("support", conversationId, currentUser.getId());
        LocalDateTime counterpartReadAt = resolveSupportCounterpartReadAt(conversation, currentUser);
        List<SupportMessageEntity> syncMessages = normalizedSinceSeq == 0L
            ? supportMessageRepository.findByConversation_IdOrderByCreatedAtAsc(conversationId)
            : supportMessageRepository.findByConversationIdSinceSeq(conversationId, normalizedSinceSeq);
        java.util.Set<String> hiddenSyncMessageIds = hiddenMessageIds(currentUser, syncMessages.stream().map(SupportMessageEntity::getId).toList());
        java.util.List<SupportMessageEntity> visibleSyncMessages = syncMessages.stream()
            .filter(message -> !hiddenSyncMessageIds.contains(message.getId()))
            .toList();
        long latestSeq = supportMessageRepository.findMaxServerSeqByConversationId(conversationId);
        long readSeq = counterpartReadAt == null ? 0L : supportMessageRepository.findReadSeqByConversationId(conversationId, counterpartReadAt);
        int unreadCount = countUnread(syncMessagesForUnread(conversationId), currentUser, lastReadAt);

        return new ChatMessageSync(
            visibleSyncMessages.stream()
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

        String outgoingContent = normalizeOutgoingSupportContent(currentUser, request.messageType(), request.content());
        ChatMessageReply replyTo = resolveReplyTo(conversation, currentUser, request.replyTo());

        SupportMessageEntity saved = appendMessageEntity(
            conversation,
            currentUser,
            staffSenderRole(currentUser),
            request.messageType(),
            outgoingContent,
            clientMessageId,
            replyTo
        );
        messageAttachmentService.createFromMessageContent("SUPPORT", saved.getId(), saved.getMessageType(), saved.getContent());

        ChatMessage message = normalizeOwnSupportMessage(saved);
        mirrorAfterCommit(saved);
        realtimeChatService.broadcast(
            RealtimeChatService.supportChannel(conversationId),
            currentUser.getId(),
            "me",
            isStaff(currentUser) ? "support" : "friend",
            saved.getMessageType().toLowerCase(),
            saved.getContent(),
            saved.getId(),
            MESSAGE_TIME_FORMATTER.format(saved.getCreatedAt()),
            saved.getClientMessageId(),
            saved.getServerSeq(),
            saved.getDeliveryStatus(),
            saved.getDeliveredAt() == null ? "" : MESSAGE_TIME_FORMATTER.format(saved.getDeliveredAt()),
            saved.getFailedReason(),
            replyToChatMessage(saved),
            message.attachments()
        );
        return message;
    }

    private String normalizeOutgoingSupportContent(UserEntity currentUser, String messageType, String content) {
        if (!isStaff(currentUser) || !"TEXT".equalsIgnoreCase(messageType == null ? "" : messageType.trim())) {
            return content;
        }
        return translationService.translateToEnglish(content).translatedText();
    }

    @Transactional
    public ChatMessage appendUserOrderMessage(
        SupportConversationEntity conversation,
        UserEntity sender,
        String content
    ) {
        return appendUserOrderMessage(conversation, sender, content, null);
    }

    @Transactional
    public ChatMessage appendUserOrderMessage(
        SupportConversationEntity conversation,
        UserEntity sender,
        String content,
        TradeOrderEntity tradeOrder
    ) {
        if (sender == null || !conversation.getCustomerUser().getId().equals(sender.getId())) {
            throw new IllegalArgumentException("Order message sender does not own the support conversation");
        }
        if (tradeOrder != null && !sender.getId().equals(tradeOrder.getOwnerUser().getId())) {
            throw new IllegalArgumentException("Order message sender does not own the trade order");
        }
        SupportMessageEntity saved = appendMessageEntity(
            conversation,
            sender,
            "ME",
            tradeOrder == null ? "TEXT" : "ORDER",
            content,
            "",
            null,
            tradeOrder
        );
        ChatMessage message = normalizeOwnSupportMessage(saved);
        if (tradeOrder == null) {
            mirrorAfterCommit(saved);
        }
        broadcastOrderMessageAfterCommit(saved);
        return message;
    }

    @Transactional
    public void publishOrderUpdate(TradeOrderEntity tradeOrder) {
        if (tradeOrder == null) {
            return;
        }
        supportMessageRepository.findByTradeOrder_Id(tradeOrder.getId())
            .ifPresent(this::broadcastOrderMessageAfterCommit);
    }

    private void broadcastOrderMessageAfterCommit(SupportMessageEntity saved) {
        Runnable broadcast = () -> realtimeChatService.broadcast(
            RealtimeChatService.supportChannel(saved.getConversation().getId()),
            saved.getSenderUser() == null ? null : saved.getSenderUser().getId(),
            "me",
            "friend",
            saved.getMessageType().toLowerCase(),
            saved.getContent(),
            saved.getId(),
            MESSAGE_TIME_FORMATTER.format(saved.getCreatedAt()),
            saved.getClientMessageId(),
            saved.getServerSeq(),
            saved.getDeliveryStatus(),
            saved.getDeliveredAt() == null ? "" : MESSAGE_TIME_FORMATTER.format(saved.getDeliveredAt()),
            saved.getFailedReason(),
            replyToChatMessage(saved),
            messageAttachmentService.attachmentsFor("SUPPORT", saved.getId(), saved.getMessageType(), saved.getContent()),
            toChatOrder(saved.getTradeOrder())
        );
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    broadcast.run();
                }
            });
        } else {
            broadcast.run();
        }
    }

    @Transactional
    public ChatMessage appendStaffMessage(
        SupportConversationEntity conversation,
        UserEntity sender,
        String senderRole,
        String messageType,
        String content
    ) {
        return appendStaffMessage(conversation, sender, senderRole, messageType, content, "");
    }

    @Transactional
    public ChatMessage appendStaffMessage(
        SupportConversationEntity conversation,
        UserEntity sender,
        String senderRole,
        String messageType,
        String content,
        String mediaUrl
    ) {
        String normalizedRole = senderRole == null ? "SUPPORT" : senderRole.trim().toUpperCase();
        SupportMessageEntity saved = appendMessageEntity(conversation, sender, normalizedRole, messageType, content, "", null);
        if (StringUtils.hasText(mediaUrl)) {
            messageAttachmentService.createFromUrl("SUPPORT", saved.getId(), saved.getMessageType(), mediaUrl);
        } else {
            messageAttachmentService.createFromMessageContent("SUPPORT", saved.getId(), saved.getMessageType(), saved.getContent());
        }
        String author = "ADMIN".equals(normalizedRole) ? "support" : normalizedRole.toLowerCase();
        List<com.cardnova.giftchat.model.MessageAttachment> attachments = messageAttachmentService.attachmentsFor(
            "SUPPORT",
            saved.getId(),
            saved.getMessageType(),
            saved.getContent()
        );

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
            saved.getFailedReason(),
            replyToChatMessage(saved),
            attachments
        );

        return new ChatMessage(
            saved.getId(),
            author,
            saved.getMessageType().toLowerCase(),
            saved.getContent(),
            MESSAGE_TIME_FORMATTER.format(saved.getCreatedAt()),
            "none",
            "",
            saved.getServerSeq() == null ? 0L : saved.getServerSeq(),
            normalizeDeliveryStatus(saved.getDeliveryStatus()),
            saved.getDeliveredAt() == null ? "" : MESSAGE_TIME_FORMATTER.format(saved.getDeliveredAt()),
            saved.getFailedReason() == null ? "" : saved.getFailedReason(),
            attachments,
            replyToChatMessage(saved)
        );
    }

    private List<SupportConversationEntity> conversationsFor(UserEntity user) {
        if ("ADMIN".equalsIgnoreCase(user.getRoleCode())) {
            return supportConversationRepository.findAllByOrderByUpdatedAtDesc();
        }
        if (isAgent(user)) {
            return supportConversationRepository.findByAssignedAgent_IdOrderByUpdatedAtDesc(user.getId());
        }
        return supportConversationRepository.findByCustomerUser_IdOrderByUpdatedAtDesc(user.getId());
    }

    private boolean shouldShowConversation(SupportConversationEntity conversation, UserEntity currentUser) {
        if (!"USER".equalsIgnoreCase(currentUser.getRoleCode())) {
            return true;
        }
        return !userHiddenRecordService.isHidden(
            currentUser.getId(),
            UserHiddenRecordService.TYPE_CONVERSATION,
            conversation.getId(),
            "CONVERSATION"
        );
    }

    @Transactional
    public SupportConversationEntity ensureUserConversation(UserEntity user) {
        if (!isCustomer(user)) {
            return null;
        }
        SupportConversationEntity existing = supportConversationRepository.findFirstByCustomerUser_IdOrderByUpdatedAtDesc(user.getId())
            .orElse(null);
        if (existing != null) {
            if (existing.getAssignedAgent() == null) {
                existing.setAssignedAgent(selectBalancedAgent());
                existing.setAssignmentStatus("AUTO_ASSIGNED");
                existing.setUpdatedAt(LocalDateTime.now());
                existing = supportConversationRepository.save(existing);
            }
            sendAgentWelcomeMessageIfNeeded(existing);
            return existing;
        }

        SupportConversationEntity conversation = new SupportConversationEntity();
        conversation.setId(UUID.randomUUID().toString());
        conversation.setCustomerUser(user);
        conversation.setAssignedAgent(selectBalancedAgent());
        conversation.setAssignmentStatus("AUTO_ASSIGNED");
        conversation.setCreatedAt(LocalDateTime.now());
        conversation.setUpdatedAt(LocalDateTime.now());
        SupportConversationEntity saved = supportConversationRepository.save(conversation);
        sendAgentWelcomeMessageIfNeeded(saved);
        return saved;
    }

    @Transactional
    public void appendSystemMessage(SupportConversationEntity conversation, String content) {
        SupportMessageEntity saved = appendMessageEntity(conversation, null, "SYSTEM", "TEXT", content, "", null);

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
            saved.getFailedReason(),
            replyToChatMessage(saved)
        );
    }

    private SupportMessageEntity appendMessageEntity(
        SupportConversationEntity conversation,
        UserEntity sender,
        String senderRole,
        String messageType,
        String content,
        String clientMessageId,
        ChatMessageReply replyTo
    ) {
        return appendMessageEntity(conversation, sender, senderRole, messageType, content, clientMessageId, replyTo, null);
    }

    private SupportMessageEntity appendMessageEntity(
        SupportConversationEntity conversation,
        UserEntity sender,
        String senderRole,
        String messageType,
        String content,
        String clientMessageId,
        ChatMessageReply replyTo,
        TradeOrderEntity tradeOrder
    ) {
        LocalDateTime now = LocalDateTime.now();
        SupportMessageEntity entity = new SupportMessageEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setConversation(conversation);
        entity.setSenderUser(sender);
        entity.setTradeOrder(tradeOrder);
        entity.setSenderRole(senderRole.trim().toUpperCase());
        entity.setMessageType(messageType.trim().toUpperCase());
        entity.setContent(content.trim());
        String normalizedClientMessageId = normalizeClientMessageId(clientMessageId);
        entity.setClientMessageId(normalizedClientMessageId.isEmpty() ? null : normalizedClientMessageId);
        if (replyTo != null) {
            entity.setReplyToMessageId(replyTo.messageId());
            entity.setReplyToAuthor(replyTo.author());
            entity.setReplyToContent(replyTo.content());
        }
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

    private void sendAgentWelcomeMessageIfNeeded(SupportConversationEntity conversation) {
        if (conversation.getWelcomeMessageSentAt() != null || conversation.getAssignedAgent() == null) {
            return;
        }
        UserEntity agent = conversation.getAssignedAgent();
        AgentWelcomeMessageEntity welcome = agentWelcomeMessageRepository.findByAgent_Id(agent.getId())
            .orElseGet(() -> defaultAgentWelcomeMessage(agent));
        if (!Boolean.TRUE.equals(welcome.getEnabled()) || !StringUtils.hasText(welcome.getContent())) {
            return;
        }

        appendStaffMessage(conversation, agent, "SUPPORT", "TEXT", welcome.getContent());
        conversation.setWelcomeMessageSentAt(LocalDateTime.now());
        conversation.setWelcomeMessageAgent(agent);
        supportConversationRepository.save(conversation);
    }

    private AgentWelcomeMessageEntity defaultAgentWelcomeMessage(UserEntity agent) {
        LocalDateTime now = LocalDateTime.now();
        AgentWelcomeMessageEntity welcome = new AgentWelcomeMessageEntity();
        welcome.setId(UUID.randomUUID().toString());
        welcome.setAgent(agent);
        welcome.setContent(AgentWelcomeMessageDefaults.CONTENT);
        welcome.setEnabled(true);
        welcome.setCreatedAt(now);
        welcome.setUpdatedAt(now);
        return welcome;
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

    private boolean isStaff(UserEntity user) {
        return isAgent(user) || "ADMIN".equalsIgnoreCase(user.getRoleCode());
    }

    private boolean isCustomer(UserEntity user) {
        return "USER".equalsIgnoreCase(user.getRoleCode());
    }

    private String staffSenderRole(UserEntity user) {
        if ("ADMIN".equalsIgnoreCase(user.getRoleCode())) {
            return "ADMIN";
        }
        if (isAgent(user)) {
            return "SUPPORT";
        }
        return "ME";
    }

    private java.util.Set<String> hiddenMessageIds(UserEntity currentUser, java.util.Collection<String> messageIds) {
        if (!"USER".equalsIgnoreCase(currentUser.getRoleCode())) {
            return java.util.Set.of();
        }
        return userHiddenRecordService.hiddenTargetIds(currentUser.getId(), UserHiddenRecordService.TYPE_MESSAGE, messageIds);
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
            message.getFailedReason() == null ? "" : message.getFailedReason(),
            messageAttachmentService.attachmentsFor("SUPPORT", message.getId(), message.getMessageType(), message.getContent()),
            replyToChatMessage(message),
            toChatOrder(message.getTradeOrder())
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
            saved.getFailedReason() == null ? "" : saved.getFailedReason(),
            messageAttachmentService.attachmentsFor("SUPPORT", saved.getId(), saved.getMessageType(), saved.getContent()),
            replyToChatMessage(saved),
            toChatOrder(saved.getTradeOrder())
        );
    }

    private ChatOrderItem toChatOrder(TradeOrderEntity order) {
        if (order == null) {
            return null;
        }
        BigDecimal estimated = order.getEstimatedLocalAmount() == null ? order.getLocalAmount() : order.getEstimatedLocalAmount();
        BigDecimal finalAmount = order.getFinalLocalAmount();
        if (finalAmount == null && "COMPLETED".equalsIgnoreCase(order.getStatusCode())) {
            finalAmount = order.getLocalAmount();
        }
        return new ChatOrderItem(
            order.getId(),
            order.getOrderNo(),
            order.getCardName(),
            order.getFaceValue(),
            decimal(estimated),
            decimal(finalAmount),
            order.getPayoutAmount(),
            order.getCurrencyCode() == null ? "" : order.getCurrencyCode(),
            order.getStatusCode().toLowerCase(),
            order.getVoucherImageUrl() == null ? "" : order.getVoucherImageUrl(),
            decimal(order.getManualVipPoints()),
            order.getSettlementReason() == null ? "" : order.getSettlementReason(),
            order.getSettledByUser() == null ? "" : order.getSettledByUser().getUsername(),
            order.getSettledAt() == null ? "" : MESSAGE_TIME_FORMATTER.format(order.getSettledAt())
        );
    }

    private String decimal(BigDecimal value) {
        return value == null ? "" : value.stripTrailingZeros().toPlainString();
    }

    private ChatMessageReply resolveReplyTo(SupportConversationEntity conversation, UserEntity currentUser, ChatMessageReply replyTo) {
        if (replyTo == null || replyTo.messageId() == null || replyTo.messageId().isBlank()) {
            return null;
        }
        SupportMessageEntity referenced = supportMessageRepository.findById(replyTo.messageId().trim())
            .orElseThrow(() -> new IllegalArgumentException("Quoted message not found"));
        if (!conversation.getId().equals(referenced.getConversation().getId())) {
            throw new IllegalArgumentException("Quoted message not found");
        }
        return new ChatMessageReply(
            referenced.getId(),
            resolveAuthor(referenced, currentUser),
            referenced.getContent()
        );
    }

    private ChatMessageReply replyToChatMessage(SupportMessageEntity message) {
        if (message.getReplyToMessageId() == null || message.getReplyToMessageId().isBlank()) {
            return null;
        }
        return new ChatMessageReply(
            message.getReplyToMessageId(),
            message.getReplyToAuthor() == null ? "" : message.getReplyToAuthor(),
            message.getReplyToContent() == null ? "" : message.getReplyToContent()
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
