package com.cardnova.giftchat.service;

import com.cardnova.giftchat.dto.LoginResponse;
import com.cardnova.giftchat.dto.RegisterRequest;
import com.cardnova.giftchat.entity.SupportConversationEntity;
import com.cardnova.giftchat.entity.InviteCodeEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PersistentAccountService {

    private static final String INVALID_LOGIN_MESSAGE = "Invalid account, password, or country";

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordService passwordService;
    private final PersistentSupportService persistentSupportService;
    private final NotificationService notificationService;
    private final LoginRateLimitService loginRateLimitService;
    private final ObjectProvider<HttpServletRequest> requestProvider;
    private final AccountProfileService accountProfileService;
    private final ReferralRewardService referralRewardService;
    private final RegistrationBonusService registrationBonusService;
    private final CountryCodeService countryCodeService;
    private final InviteCodeService inviteCodeService;

    public PersistentAccountService(
        UserRepository userRepository,
        JwtService jwtService,
        PasswordService passwordService,
        PersistentSupportService persistentSupportService,
        NotificationService notificationService,
        LoginRateLimitService loginRateLimitService,
        ObjectProvider<HttpServletRequest> requestProvider,
        AccountProfileService accountProfileService,
        ReferralRewardService referralRewardService,
        RegistrationBonusService registrationBonusService,
        CountryCodeService countryCodeService,
        InviteCodeService inviteCodeService
    ) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordService = passwordService;
        this.persistentSupportService = persistentSupportService;
        this.notificationService = notificationService;
        this.loginRateLimitService = loginRateLimitService;
        this.requestProvider = requestProvider;
        this.accountProfileService = accountProfileService;
        this.referralRewardService = referralRewardService;
        this.registrationBonusService = registrationBonusService;
        this.countryCodeService = countryCodeService;
        this.inviteCodeService = inviteCodeService;
    }

    public Optional<UserEntity> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional
    public LoginResponse login(String identifier, String password, String countryCode) {
        String normalizedIdentifier = requireTrimmed(identifier, "Identifier is required");
        String normalizedPassword = requireTrimmed(password, "Password is required");
        String clientIp = clientIp();

        loginRateLimitService.checkAllowed(normalizedIdentifier, clientIp);

        try {
            UserEntity user = findByIdentifier(normalizedIdentifier, countryCode)
                .orElseThrow(this::invalidLogin);

            if (!"ACTIVE".equalsIgnoreCase(user.getStatusCode())) {
                throw invalidLogin();
            }

            if (!passwordService.matches(normalizedPassword, user.getPasswordHash())) {
                throw invalidLogin();
            }

            if ("USER".equalsIgnoreCase(user.getRoleCode())) {
                ensureCountryBinding(user);
                String requestedCountry = countryCodeService.requireCountry(countryCode).code();
                if (!requestedCountry.equals(user.getCountryCode())) {
                    throw invalidLogin();
                }
            }

            loginRateLimitService.recordSuccess(normalizedIdentifier, clientIp);
            return accountProfileService.toSession(user);
        } catch (IllegalArgumentException exception) {
            if (INVALID_LOGIN_MESSAGE.equals(exception.getMessage())) {
                loginRateLimitService.recordFailure(normalizedIdentifier, clientIp);
            }
            throw exception;
        }
    }

    private IllegalArgumentException invalidLogin() {
        return new IllegalArgumentException(INVALID_LOGIN_MESSAGE);
    }

    @Transactional
    public UserEntity register(RegisterRequest request) {
        String username = requireTrimmed(request.username(), "Username is required");
        String password = requireTrimmed(request.password(), "Password is required");
        String email = normalizeNullable(request.email());
        var country = countryCodeService.requireCountry(request.countryCode());
        String phone = countryCodeService.normalizeRegistrationPhone(request.phone(), country.code());

        if (!StringUtils.hasText(email) && !StringUtils.hasText(phone)) {
            throw new IllegalArgumentException("Email or phone is required");
        }

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (StringUtils.hasText(email) && userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (StringUtils.hasText(phone) && userRepository.findByPhone(phone).isPresent()) {
            throw new IllegalArgumentException("Phone already exists");
        }

        InviteCodeEntity registrationInviteCode = inviteCodeService.resolveForRegistration(request.inviteCode());
        UserEntity entity = new UserEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setUsername(username);
        entity.setEmail(email);
        entity.setPhone(phone);
        entity.setPasswordHash(passwordService.hash(password));
        entity.setRoleCode("USER");
        entity.setStatusCode("ACTIVE");
        entity.setCountryCode(country.code());
        entity.setCurrencyCode(country.currencyCode());
        entity.setCountryBindingStatus("BOUND");
        entity.setCountryBoundAt(LocalDateTime.now());
        entity.setInviteCode(referralRewardService.generateInviteCode(username));
        entity.setOnboardingPolicyVersion(1);
        entity.setLotteryAccessStatus("APPROVED");
        entity.setRegistrationInviteCode(registrationInviteCode);
        if (registrationInviteCode != null
            && InviteCodeService.PERSONAL.equals(registrationInviteCode.getCodeType())
            && registrationInviteCode.getOwnerUser() != null) {
            entity.setReferredByUserId(registrationInviteCode.getOwnerUser().getId());
        }
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        UserEntity saved = userRepository.save(entity);
        inviteCodeService.registerPersonalCode(saved);
        return saved;
    }

    @Transactional
    public LoginResponse registerAndLogin(RegisterRequest request) {
        UserEntity user = register(request);
        referralRewardService.rewardRegistration(user);
        registrationBonusService.awardRegistrationBonus(user);
        handleUserAccessEvent(user, "REGISTER", "New user registered", "User " + user.getUsername() + " registered.");
        return accountProfileService.toSession(user);
    }

    private void handleUserAccessEvent(UserEntity user, String eventType, String title, String body) {
        if (!"USER".equalsIgnoreCase(user.getRoleCode())) {
            return;
        }

        SupportConversationEntity conversation = persistentSupportService.ensureUserConversation(user);
        if (conversation == null) {
            return;
        }

        persistentSupportService.appendSystemMessage(conversation, body);
        if (conversation.getAssignedAgent() != null) {
            notificationService.notifyUser(
                conversation.getAssignedAgent(),
                user,
                eventType,
                title,
                body,
                "SUPPORT_CONVERSATION",
                conversation.getId()
            );
        }
        notificationService.notifyAdmins(
            user,
            eventType,
            title,
            body,
            "SUPPORT_CONVERSATION",
            conversation.getId()
        );
    }

    private Optional<UserEntity> findByIdentifier(String identifier, String countryCode) {
        if (!StringUtils.hasText(identifier)) {
            return Optional.empty();
        }

        return userRepository.findByUsername(identifier)
            .or(() -> userRepository.findByEmailIgnoreCase(identifier))
            .or(() -> findByPhone(identifier, countryCode));
    }

    private Optional<UserEntity> findByPhone(String identifier, String countryCode) {
        try {
            String normalizedPhone = countryCodeService.normalizeLoginPhone(identifier, countryCode);
            return StringUtils.hasText(normalizedPhone)
                ? userRepository.findByNormalizedPhone(normalizedPhone)
                : Optional.empty();
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private void ensureCountryBinding(UserEntity user) {
        if ("BOUND".equalsIgnoreCase(user.getCountryBindingStatus())
            && StringUtils.hasText(user.getCountryCode())
            && StringUtils.hasText(user.getCurrencyCode())) {
            return;
        }
        var resolved = countryCodeService.resolvePhoneCountry(user.getPhone());
        if (resolved.isEmpty()) {
            user.setCountryBindingStatus("UNRESOLVED");
            userRepository.save(user);
            throw invalidLogin();
        }
        var country = resolved.get();
        user.setCountryCode(country.code());
        user.setCurrencyCode(country.currencyCode());
        user.setCountryBindingStatus("BOUND");
        user.setCountryBoundAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String requireTrimmed(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String clientIp() {
        HttpServletRequest request = requestProvider.getIfAvailable();
        if (request == null) {
            return "unknown";
        }
        return request.getRemoteAddr();
    }
}
