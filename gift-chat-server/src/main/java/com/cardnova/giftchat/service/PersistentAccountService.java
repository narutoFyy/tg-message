package com.cardnova.giftchat.service;

import com.cardnova.giftchat.dto.LoginResponse;
import com.cardnova.giftchat.dto.RegisterRequest;
import com.cardnova.giftchat.entity.SupportConversationEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class PersistentAccountService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordService passwordService;
    private final PersistentSupportService persistentSupportService;
    private final NotificationService notificationService;

    public PersistentAccountService(
        UserRepository userRepository,
        JwtService jwtService,
        PasswordService passwordService,
        PersistentSupportService persistentSupportService,
        NotificationService notificationService
    ) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.passwordService = passwordService;
        this.persistentSupportService = persistentSupportService;
        this.notificationService = notificationService;
    }

    public Optional<UserEntity> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Transactional
    public LoginResponse login(String identifier, String password) {
        String normalizedIdentifier = requireTrimmed(identifier, "Identifier is required");
        String normalizedPassword = requireTrimmed(password, "Password is required");

        UserEntity user = findByIdentifier(normalizedIdentifier)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        if (!"ACTIVE".equalsIgnoreCase(user.getStatusCode())) {
            throw new IllegalArgumentException("User is not active");
        }

        if (!passwordService.matches(normalizedPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Password mismatch");
        }

        handleUserAccessEvent(user, "LOGIN", "User login", "User " + user.getUsername() + " logged in.");

        return new LoginResponse(
            jwtService.issueAccessToken(user),
            user.getUsername(),
            user.getEmail(),
            user.getPhone(),
            user.getRoleCode(),
            nextRoute(user),
            jwtService.getAccessTokenExpiry().toString()
        );
    }

    @Transactional
    public UserEntity register(RegisterRequest request) {
        String username = requireTrimmed(request.username(), "Username is required");
        String password = requireTrimmed(request.password(), "Password is required");
        String email = normalizeNullable(request.email());
        String phone = normalizeNullable(request.phone());

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

        UserEntity entity = new UserEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setUsername(username);
        entity.setEmail(email);
        entity.setPhone(phone);
        entity.setPasswordHash(passwordService.hash(password));
        entity.setRoleCode("USER");
        entity.setStatusCode("ACTIVE");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return userRepository.save(entity);
    }

    @Transactional
    public LoginResponse registerAndLogin(RegisterRequest request) {
        UserEntity user = register(request);
        handleUserAccessEvent(user, "REGISTER", "New user registered", "User " + user.getUsername() + " registered.");
        return new LoginResponse(
            jwtService.issueAccessToken(user),
            user.getUsername(),
            user.getEmail(),
            user.getPhone(),
            user.getRoleCode(),
            nextRoute(user),
            jwtService.getAccessTokenExpiry().toString()
        );
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

    private Optional<UserEntity> findByIdentifier(String identifier) {
        if (!StringUtils.hasText(identifier)) {
            return Optional.empty();
        }

        return userRepository.findByUsername(identifier)
            .or(() -> userRepository.findByEmail(identifier))
            .or(() -> userRepository.findByPhone(identifier));
    }

    private String nextRoute(UserEntity user) {
        if ("ADMIN".equalsIgnoreCase(user.getRoleCode())) {
            return "/pages/admin-rates/index";
        }
        if ("AGENT".equalsIgnoreCase(user.getRoleCode())) {
            return "/pages/support-chat-v2/index";
        }
        return "/pages/support/index";
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
}
