package com.cardnova.giftchat.service;

import com.cardnova.giftchat.dto.CreatePromotionInviteCodeRequest;
import com.cardnova.giftchat.entity.InviteCodeEntity;
import com.cardnova.giftchat.entity.SupportConversationEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.model.PromotionInviteCodeItem;
import com.cardnova.giftchat.model.PromotionInviteRegistrationItem;
import com.cardnova.giftchat.model.PromotionInviteRegistrationPage;
import com.cardnova.giftchat.repository.InviteCodeRepository;
import com.cardnova.giftchat.repository.SupportConversationRepository;
import com.cardnova.giftchat.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PromotionInviteCodeAdminService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int MAX_PAGE_SIZE = 100;

    private final InviteCodeRepository inviteCodeRepository;
    private final UserRepository userRepository;
    private final SupportConversationRepository supportConversationRepository;
    private final CurrentUserService currentUserService;
    private final InviteCodeService inviteCodeService;

    public PromotionInviteCodeAdminService(
        InviteCodeRepository inviteCodeRepository,
        UserRepository userRepository,
        SupportConversationRepository supportConversationRepository,
        CurrentUserService currentUserService,
        InviteCodeService inviteCodeService
    ) {
        this.inviteCodeRepository = inviteCodeRepository;
        this.userRepository = userRepository;
        this.supportConversationRepository = supportConversationRepository;
        this.currentUserService = currentUserService;
        this.inviteCodeService = inviteCodeService;
    }

    public List<PromotionInviteCodeItem> list() {
        requireAdmin();
        return inviteCodeRepository.findByCodeTypeOrderByCreatedAtDesc(InviteCodeService.PROMOTION).stream()
            .map(this::toItem)
            .toList();
    }

    @Transactional
    public PromotionInviteCodeItem create(CreatePromotionInviteCodeRequest request) {
        UserEntity admin = requireAdmin();
        String rawCode = request.code().trim();
        if (!rawCode.matches("[A-Za-z0-9]+")) {
            throw new IllegalArgumentException("Promotion invite code may contain letters and numbers only");
        }
        String code = inviteCodeService.normalize(request.code());
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("Promotion invite code must contain letters or numbers");
        }
        if (code.length() > 32) {
            throw new IllegalArgumentException("Promotion invite code must be 32 characters or less");
        }
        if (inviteCodeRepository.existsById(code) || userRepository.findByInviteCode(code).isPresent()) {
            throw new IllegalArgumentException("Invite code already exists");
        }

        LocalDateTime now = LocalDateTime.now();
        InviteCodeEntity entity = new InviteCodeEntity();
        entity.setCode(code);
        entity.setCodeType(InviteCodeService.PROMOTION);
        entity.setEnabled(true);
        entity.setCreatedByUser(admin);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return toItem(inviteCodeRepository.save(entity));
    }

    @Transactional
    public PromotionInviteCodeItem updateStatus(String rawCode, boolean enabled) {
        requireAdmin();
        InviteCodeEntity entity = requirePromotionCode(rawCode);
        entity.setEnabled(enabled);
        entity.setUpdatedAt(LocalDateTime.now());
        return toItem(inviteCodeRepository.save(entity));
    }

    public PromotionInviteRegistrationPage registrations(String rawCode, int requestedPage, int requestedPageSize) {
        requireAdmin();
        InviteCodeEntity entity = requirePromotionCode(rawCode);
        int page = Math.max(0, requestedPage);
        int pageSize = Math.max(1, Math.min(requestedPageSize, MAX_PAGE_SIZE));
        var result = userRepository.findByRegistrationInviteCode_CodeOrderByCreatedAtDesc(
            entity.getCode(),
            PageRequest.of(page, pageSize)
        );
        return new PromotionInviteRegistrationPage(
            entity.getCode(),
            result.getTotalElements(),
            result.getNumber(),
            result.getSize(),
            result.getTotalPages(),
            result.getContent().stream().map(this::toRegistrationItem).toList()
        );
    }

    private InviteCodeEntity requirePromotionCode(String rawCode) {
        String code = inviteCodeService.normalize(rawCode);
        InviteCodeEntity entity = StringUtils.hasText(code)
            ? inviteCodeRepository.findById(code).orElse(null)
            : null;
        if (entity == null || !InviteCodeService.PROMOTION.equals(entity.getCodeType())) {
            throw new IllegalArgumentException("Promotion invite code not found");
        }
        return entity;
    }

    private PromotionInviteCodeItem toItem(InviteCodeEntity entity) {
        return new PromotionInviteCodeItem(
            entity.getCode(),
            entity.getCodeType().toLowerCase(),
            entity.isEnabled(),
            userRepository.countByRegistrationInviteCode_Code(entity.getCode()),
            entity.getCreatedByUser() == null ? "" : entity.getCreatedByUser().getUsername(),
            TIME_FORMATTER.format(entity.getCreatedAt()),
            TIME_FORMATTER.format(entity.getUpdatedAt())
        );
    }

    private PromotionInviteRegistrationItem toRegistrationItem(UserEntity user) {
        String assignedAgent = supportConversationRepository.findFirstByCustomerUser_IdOrderByUpdatedAtDesc(user.getId())
            .map(SupportConversationEntity::getAssignedAgent)
            .map(UserEntity::getUsername)
            .orElse("");
        return new PromotionInviteRegistrationItem(
            user.getId(),
            user.getUsername(),
            assignedAgent,
            TIME_FORMATTER.format(user.getCreatedAt())
        );
    }

    private UserEntity requireAdmin() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        currentUserService.requireAdmin(currentUser);
        return currentUser;
    }
}
