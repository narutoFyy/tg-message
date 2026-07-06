package com.cardnova.giftchat.service;

import com.cardnova.giftchat.dto.CreateBroadcastRequest;
import com.cardnova.giftchat.entity.BroadcastMessageEntity;
import com.cardnova.giftchat.entity.SupportConversationEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.model.BroadcastItem;
import com.cardnova.giftchat.repository.BroadcastMessageRepository;
import com.cardnova.giftchat.repository.SupportConversationRepository;
import com.cardnova.giftchat.repository.UserRepository;
import com.cardnova.giftchat.repository.WithdrawalRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
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
    private final PhoneCountryCodeResolver phoneCountryCodeResolver;
    private final RegistrationBonusService registrationBonusService;
    private final WithdrawalRequestRepository withdrawalRequestRepository;

    public BroadcastService(
        BroadcastMessageRepository broadcastMessageRepository,
        SupportConversationRepository supportConversationRepository,
        UserRepository userRepository,
        CurrentUserService currentUserService,
        PersistentSupportService persistentSupportService,
        NotificationService notificationService,
        PhoneCountryCodeResolver phoneCountryCodeResolver,
        RegistrationBonusService registrationBonusService,
        WithdrawalRequestRepository withdrawalRequestRepository
    ) {
        this.broadcastMessageRepository = broadcastMessageRepository;
        this.supportConversationRepository = supportConversationRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.persistentSupportService = persistentSupportService;
        this.notificationService = notificationService;
        this.phoneCountryCodeResolver = phoneCountryCodeResolver;
        this.registrationBonusService = registrationBonusService;
        this.withdrawalRequestRepository = withdrawalRequestRepository;
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
        BroadcastFilters filters = normalizeFilters(request);
        List<SupportConversationEntity> targets = resolveTargets(currentUser, scope, filters);

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
        entity.setCountryCodes(String.join(",", filters.countryCodes()));
        entity.setSearchKeyword(filters.keyword());
        entity.setTargetMode(filters.targetConversationIds().isEmpty() ? "FILTER" : "EXPLICIT");
        entity.setTargetUsernames(String.join(",", targets.stream().map(item -> item.getCustomerUser().getUsername()).toList()));
        entity.setCreatedAt(LocalDateTime.now());
        return toItem(broadcastMessageRepository.save(entity));
    }

    private List<SupportConversationEntity> resolveTargets(UserEntity currentUser, String scope, BroadcastFilters filters) {
        List<SupportConversationEntity> base;
        if ("own".equals(scope)) {
            base = supportConversationRepository.findByAssignedAgent_IdOrderByUpdatedAtDesc(currentUser.getId());
        } else {
            base = userRepository.findByRoleCodeAndStatusCodeOrderByCreatedAtAsc("USER", "ACTIVE").stream()
                .map(persistentSupportService::ensureUserConversation)
                .toList();
        }

        return base.stream()
            .filter(conversation -> conversation != null && "ACTIVE".equalsIgnoreCase(conversation.getCustomerUser().getStatusCode()))
            .filter(conversation -> filters.targetConversationIds().isEmpty() || filters.targetConversationIds().contains(conversation.getId()))
            .filter(conversation -> filters.countryCodes().isEmpty() || filters.countryCodes().contains(countryCode(conversation.getCustomerUser())))
            .filter(conversation -> filters.keyword().isBlank() || matchesKeyword(conversation, filters.keyword()))
            .toList();
    }

    private BroadcastFilters normalizeFilters(CreateBroadcastRequest request) {
        List<String> countryCodes = request.countryCodes() == null ? List.of() : request.countryCodes().stream()
            .map(phoneCountryCodeResolver::normalizeCountryCode)
            .filter(StringUtils::hasText)
            .distinct()
            .toList();
        List<String> targetConversationIds = request.targetConversationIds() == null ? List.of() : request.targetConversationIds().stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .distinct()
            .toList();
        String keyword = request.keyword() == null ? "" : request.keyword().trim();
        if (keyword.length() > 128) {
            keyword = keyword.substring(0, 128);
        }
        return new BroadcastFilters(countryCodes, keyword, targetConversationIds);
    }

    private boolean matchesKeyword(SupportConversationEntity conversation, String keyword) {
        String normalized = keyword.toLowerCase(Locale.ROOT);
        UserEntity customer = conversation.getCustomerUser();
        if (contains(customer.getUsername(), normalized)
            || contains(customer.getPhone(), normalized)
            || contains(conversation.getAgentNote(), normalized)
            || contains(countryCode(customer), normalized)) {
            return true;
        }
        return withdrawalRequestRepository.findByOwnerUser_IdOrderByUpdatedAtDesc(customer.getId()).stream()
            .anyMatch(withdrawal -> contains(withdrawal.getAccountName(), normalized)
                || contains(withdrawal.getBankName(), normalized)
                || contains(withdrawal.getAccountNumber(), normalized));
    }

    private boolean contains(String value, String normalizedKeyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(normalizedKeyword);
    }

    private String countryCode(UserEntity customer) {
        return phoneCountryCodeResolver.resolve(customer.getPhone(), registrationBonusService.configuredCountryCodes());
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
            entity.getCountryCodes() == null ? "" : entity.getCountryCodes(),
            entity.getSearchKeyword() == null ? "" : entity.getSearchKeyword(),
            entity.getTargetMode() == null ? "filter" : entity.getTargetMode().toLowerCase(),
            entity.getTargetUsernames() == null ? "" : entity.getTargetUsernames(),
            TIME_FORMATTER.format(entity.getCreatedAt())
        );
    }

    private record BroadcastFilters(List<String> countryCodes, String keyword, List<String> targetConversationIds) {
    }
}
