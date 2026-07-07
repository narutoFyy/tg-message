package com.cardnova.giftchat.service;

import com.cardnova.giftchat.entity.SupportConversationEntity;
import com.cardnova.giftchat.entity.SupportMessageEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.model.SupportConversation;
import com.cardnova.giftchat.model.SupportCustomerSearchResult;
import com.cardnova.giftchat.model.SupportMessageSearchResult;
import com.cardnova.giftchat.model.VipSummary;
import com.cardnova.giftchat.repository.SupportConversationRepository;
import com.cardnova.giftchat.repository.SupportMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class SupportSearchService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CurrentUserService currentUserService;
    private final SupportConversationRepository supportConversationRepository;
    private final SupportMessageRepository supportMessageRepository;
    private final PersistentSupportService persistentSupportService;
    private final PhoneCountryCodeResolver phoneCountryCodeResolver;
    private final RegistrationBonusService registrationBonusService;
    private final VipService vipService;

    public SupportSearchService(
        CurrentUserService currentUserService,
        SupportConversationRepository supportConversationRepository,
        SupportMessageRepository supportMessageRepository,
        PersistentSupportService persistentSupportService,
        PhoneCountryCodeResolver phoneCountryCodeResolver,
        RegistrationBonusService registrationBonusService,
        VipService vipService
    ) {
        this.currentUserService = currentUserService;
        this.supportConversationRepository = supportConversationRepository;
        this.supportMessageRepository = supportMessageRepository;
        this.persistentSupportService = persistentSupportService;
        this.phoneCountryCodeResolver = phoneCountryCodeResolver;
        this.registrationBonusService = registrationBonusService;
        this.vipService = vipService;
    }

    public List<SupportCustomerSearchResult> searchCustomers(String keyword) {
        UserEntity currentUser = currentStaff();
        String normalized = normalizeKeyword(keyword);
        if (normalized.isEmpty()) {
            return List.of();
        }
        return accessibleConversations(currentUser).stream()
            .filter(conversation -> customerMatches(conversation, normalized))
            .limit(50)
            .map(this::toCustomerResult)
            .toList();
    }

    public List<SupportMessageSearchResult> searchMessages(String keyword) {
        UserEntity currentUser = currentStaff();
        String normalized = normalizeKeyword(keyword);
        if (normalized.isEmpty()) {
            return List.of();
        }
        return accessibleConversations(currentUser).stream()
            .flatMap(conversation -> supportMessageRepository.findByConversation_IdOrderByCreatedAtAsc(conversation.getId()).stream()
                .filter(message -> messageMatches(message, normalized))
                .map(message -> toMessageResult(conversation, message, normalized)))
            .limit(50)
            .toList();
    }

    private UserEntity currentStaff() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        currentUserService.requireAgentOrAdmin(currentUser);
        return currentUser;
    }

    private List<SupportConversationEntity> accessibleConversations(UserEntity currentUser) {
        if ("ADMIN".equalsIgnoreCase(currentUser.getRoleCode())) {
            return supportConversationRepository.findAllByOrderByUpdatedAtDesc();
        }
        return supportConversationRepository.findByAssignedAgent_IdOrderByUpdatedAtDesc(currentUser.getId());
    }

    private boolean customerMatches(SupportConversationEntity conversation, String keyword) {
        UserEntity customer = conversation.getCustomerUser();
        VipSummary vip = vipService.summaryForUser(customer.getId());
        String countryCode = phoneCountryCodeResolver.resolve(customer.getPhone(), registrationBonusService.configuredCountryCodes());
        return contains(customer.getUsername(), keyword)
            || contains(customer.getEmail(), keyword)
            || contains(customer.getPhone(), keyword)
            || contains(countryCode, keyword)
            || contains(conversation.getAgentNote(), keyword)
            || contains(vip.level(), keyword)
            || contains(vip.points(), keyword);
    }

    private SupportCustomerSearchResult toCustomerResult(SupportConversationEntity conversation) {
        SupportConversation item = persistentSupportService.toSupportConversation(conversation);
        UserEntity customer = conversation.getCustomerUser();
        VipSummary vip = vipService.summaryForUser(customer.getId());
        return new SupportCustomerSearchResult(
            item.conversationId(),
            item.customerUsername(),
            StringUtils.hasText(item.agentNote()) ? item.agentNote() : item.customerUsername(),
            customer.getPhone() == null ? "" : customer.getPhone(),
            item.phoneCountryCode(),
            customer.getEmail() == null ? "" : customer.getEmail(),
            vip.level(),
            vip.points(),
            item.unreadCount(),
            item.lastMessageTime(),
            item.online()
        );
    }

    private boolean messageMatches(SupportMessageEntity message, String keyword) {
        return contains(message.getContent(), keyword)
            || contains(message.getReplyToContent(), keyword)
            || contains(message.getSenderRole(), keyword);
    }

    private SupportMessageSearchResult toMessageResult(
        SupportConversationEntity conversation,
        SupportMessageEntity message,
        String keyword
    ) {
        UserEntity customer = conversation.getCustomerUser();
        String countryCode = phoneCountryCodeResolver.resolve(customer.getPhone(), registrationBonusService.configuredCountryCodes());
        String displayName = StringUtils.hasText(conversation.getAgentNote()) ? conversation.getAgentNote() : customer.getUsername();
        return new SupportMessageSearchResult(
            conversation.getId(),
            message.getId(),
            customer.getUsername(),
            displayName,
            countryCode,
            message.getSenderRole().toLowerCase(Locale.ROOT),
            snippet(message.getContent(), keyword),
            TIME_FORMATTER.format(message.getCreatedAt())
        );
    }

    private String snippet(String content, String keyword) {
        String normalizedContent = content == null ? "" : content.replaceAll("\\s+", " ").trim();
        if (normalizedContent.length() <= 120) {
            return normalizedContent;
        }
        int index = normalizedContent.toLowerCase(Locale.ROOT).indexOf(keyword);
        int start = index < 0 ? 0 : Math.max(0, index - 40);
        int end = Math.min(normalizedContent.length(), start + 120);
        return (start > 0 ? "..." : "") + normalizedContent.substring(start, end) + (end < normalizedContent.length() ? "..." : "");
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(keyword);
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
    }
}
