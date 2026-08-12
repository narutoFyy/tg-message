package com.cardnova.giftchat.service;

import com.cardnova.giftchat.entity.InviteCodeEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.repository.InviteCodeRepository;
import com.cardnova.giftchat.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class InviteCodeService {

    public static final String PERSONAL = "PERSONAL";
    public static final String PROMOTION = "PROMOTION";
    private static final int USERNAME_PREFIX_LENGTH = 12;
    private static final int RANDOM_SUFFIX_LENGTH = 3;
    private static final int GENERATION_ATTEMPTS = 100;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final InviteCodeRepository inviteCodeRepository;
    private final UserRepository userRepository;

    public InviteCodeService(InviteCodeRepository inviteCodeRepository, UserRepository userRepository) {
        this.inviteCodeRepository = inviteCodeRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public InviteCodeEntity resolveForRegistration(String value) {
        String code = normalize(value);
        if (!StringUtils.hasText(code)) {
            return null;
        }
        InviteCodeEntity inviteCode = inviteCodeRepository.findById(code)
            .orElseGet(() -> registerLegacyPersonalCode(code));
        if (!inviteCode.isEnabled()) {
            throw new IllegalArgumentException("Invite code is disabled");
        }
        return inviteCode;
    }

    private InviteCodeEntity registerLegacyPersonalCode(String code) {
        UserEntity owner = userRepository.findByInviteCode(code)
            .orElseThrow(() -> new IllegalArgumentException("Invite code not found"));
        return registerPersonalCode(owner);
    }

    public String generatePersonalCode(String username) {
        String prefix = usernamePrefix(username);
        for (int attempt = 0; attempt < GENERATION_ATTEMPTS; attempt++) {
            String candidate = prefix + randomSuffix();
            if (!inviteCodeRepository.existsById(candidate) && userRepository.findByInviteCode(candidate).isEmpty()) {
                return candidate;
            }
        }
        throw new IllegalStateException("Unable to generate a unique invite code");
    }

    @Transactional
    public InviteCodeEntity registerPersonalCode(UserEntity owner) {
        if (owner == null || !StringUtils.hasText(owner.getInviteCode())) {
            throw new IllegalArgumentException("Personal invite code is required");
        }
        String code = normalize(owner.getInviteCode());
        InviteCodeEntity existing = inviteCodeRepository.findById(code).orElse(null);
        if (existing != null) {
            if (PERSONAL.equals(existing.getCodeType())
                && existing.getOwnerUser() != null
                && owner.getId().equals(existing.getOwnerUser().getId())) {
                return existing;
            }
            throw new IllegalArgumentException("Invite code already exists");
        }

        LocalDateTime now = LocalDateTime.now();
        InviteCodeEntity entity = new InviteCodeEntity();
        entity.setCode(code);
        entity.setCodeType(PERSONAL);
        entity.setOwnerUser(owner);
        entity.setEnabled(true);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return inviteCodeRepository.save(entity);
    }

    public String invitedByLabel(UserEntity user) {
        if (user == null) {
            return "";
        }
        InviteCodeEntity source = StringUtils.hasText(user.getId())
            ? inviteCodeRepository.findRegistrationSourceByUserId(user.getId()).orElse(null)
            : user.getRegistrationInviteCode();
        if (source != null) {
            if (PROMOTION.equals(source.getCodeType())) {
                return source.getCode();
            }
            if (source.getOwnerUser() != null) {
                return "@" + source.getOwnerUser().getUsername();
            }
        }
        if (StringUtils.hasText(user.getReferredByUserId())) {
            return userRepository.findById(user.getReferredByUserId())
                .map(referrer -> "@" + referrer.getUsername())
                .orElse("");
        }
        return "";
    }

    public String sourceType(UserEntity user) {
        InviteCodeEntity source = user != null && StringUtils.hasText(user.getId())
            ? inviteCodeRepository.findRegistrationSourceByUserId(user.getId()).orElse(null)
            : user == null ? null : user.getRegistrationInviteCode();
        if (source != null) {
            return source.getCodeType();
        }
        return user != null && StringUtils.hasText(user.getReferredByUserId()) ? PERSONAL : "DIRECT";
    }

    public String normalize(String value) {
        return StringUtils.hasText(value)
            ? value.trim().replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT)
            : null;
    }

    private String usernamePrefix(String username) {
        String normalized = normalize(username);
        if (!StringUtils.hasText(normalized)) {
            normalized = "USER";
        }
        return normalized.substring(0, Math.min(normalized.length(), USERNAME_PREFIX_LENGTH));
    }

    private String randomSuffix() {
        StringBuilder suffix = new StringBuilder(RANDOM_SUFFIX_LENGTH);
        for (int index = 0; index < RANDOM_SUFFIX_LENGTH; index++) {
            suffix.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return suffix.toString();
    }
}
