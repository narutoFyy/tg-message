package com.cardnova.giftchat.service;

import com.cardnova.giftchat.dto.CreateAgentRequest;
import com.cardnova.giftchat.entity.DirectMessageEntity;
import com.cardnova.giftchat.entity.FriendshipEntity;
import com.cardnova.giftchat.entity.SupportConversationEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.model.AdminDirectConversation;
import com.cardnova.giftchat.model.AdminUserItem;
import com.cardnova.giftchat.model.AgentItem;
import com.cardnova.giftchat.model.ChatMessage;
import com.cardnova.giftchat.model.SupportConversation;
import com.cardnova.giftchat.repository.BlacklistEntryRepository;
import com.cardnova.giftchat.repository.DirectMessageRepository;
import com.cardnova.giftchat.repository.FriendshipRepository;
import com.cardnova.giftchat.repository.SupportConversationRepository;
import com.cardnova.giftchat.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class AdminService {

    private static final DateTimeFormatter USER_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter MESSAGE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    private final UserRepository userRepository;
    private final BlacklistEntryRepository blacklistEntryRepository;
    private final FriendshipRepository friendshipRepository;
    private final DirectMessageRepository directMessageRepository;
    private final SupportConversationRepository supportConversationRepository;
    private final PersistentSupportService persistentSupportService;
    private final CurrentUserService currentUserService;
    private final PasswordService passwordService;

    public AdminService(
        UserRepository userRepository,
        BlacklistEntryRepository blacklistEntryRepository,
        FriendshipRepository friendshipRepository,
        DirectMessageRepository directMessageRepository,
        SupportConversationRepository supportConversationRepository,
        PersistentSupportService persistentSupportService,
        CurrentUserService currentUserService,
        PasswordService passwordService
    ) {
        this.userRepository = userRepository;
        this.blacklistEntryRepository = blacklistEntryRepository;
        this.friendshipRepository = friendshipRepository;
        this.directMessageRepository = directMessageRepository;
        this.supportConversationRepository = supportConversationRepository;
        this.persistentSupportService = persistentSupportService;
        this.currentUserService = currentUserService;
        this.passwordService = passwordService;
    }

    public List<AdminUserItem> users() {
        requireAdmin();
        return userRepository.findAll().stream()
            .map(user -> new AdminUserItem(
                user.getId(),
                user.getUsername(),
                user.getEmail() == null ? "" : user.getEmail(),
                user.getPhone() == null ? "" : user.getPhone(),
                user.getRoleCode(),
                user.getStatusCode(),
                blacklistEntryRepository.existsByBlockedUser_Id(user.getId()),
                USER_TIME_FORMATTER.format(user.getCreatedAt())
            ))
            .toList();
    }

    public List<AgentItem> agents() {
        requireAdmin();
        return userRepository.findByRoleCodeOrderByCreatedAtDesc("AGENT").stream()
            .map(agent -> new AgentItem(
                agent.getId(),
                agent.getUsername(),
                agent.getEmail() == null ? "" : agent.getEmail(),
                agent.getPhone() == null ? "" : agent.getPhone(),
                agent.getStatusCode(),
                supportConversationRepository.countByAssignedAgent_Id(agent.getId())
            ))
            .toList();
    }

    @Transactional
    public AgentItem createAgent(CreateAgentRequest request) {
        requireAdmin();
        String username = requireTrimmed(request.username(), "Agent username is required");
        String email = normalizeNullable(request.email());
        String phone = normalizeNullable(request.phone());
        String password = requireTrimmed(request.password(), "Agent password is required");

        if (userRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (StringUtils.hasText(email) && userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (StringUtils.hasText(phone) && userRepository.findByPhone(phone).isPresent()) {
            throw new IllegalArgumentException("Phone already exists");
        }

        UserEntity agent = new UserEntity();
        agent.setId(UUID.randomUUID().toString());
        agent.setUsername(username);
        agent.setEmail(email);
        agent.setPhone(phone);
        agent.setPasswordHash(passwordService.hash(password));
        agent.setRoleCode("AGENT");
        agent.setStatusCode("ACTIVE");
        agent.setCreatedAt(LocalDateTime.now());
        agent.setUpdatedAt(LocalDateTime.now());
        UserEntity saved = userRepository.save(agent);

        return new AgentItem(saved.getId(), saved.getUsername(), value(saved.getEmail()), value(saved.getPhone()), saved.getStatusCode(), 0);
    }

    @Transactional
    public AgentItem updateAgentStatus(String agentId, String status) {
        requireAdmin();
        UserEntity agent = userRepository.findById(agentId)
            .orElseThrow(() -> new IllegalArgumentException("Agent not found"));
        if (!"AGENT".equalsIgnoreCase(agent.getRoleCode())) {
            throw new IllegalArgumentException("User is not an agent");
        }
        String normalizedStatus = normalizeStatus(status);
        agent.setStatusCode(normalizedStatus);
        agent.setUpdatedAt(LocalDateTime.now());
        UserEntity saved = userRepository.save(agent);
        return new AgentItem(
            saved.getId(),
            saved.getUsername(),
            value(saved.getEmail()),
            value(saved.getPhone()),
            saved.getStatusCode(),
            supportConversationRepository.countByAssignedAgent_Id(saved.getId())
        );
    }

    public List<SupportConversation> supportConversations() {
        requireAdmin();
        return supportConversationRepository.findAllByOrderByUpdatedAtDesc().stream()
            .map(persistentSupportService::toSupportConversation)
            .toList();
    }

    public List<AdminDirectConversation> directConversations(String username) {
        requireAdmin();
        String normalized = normalizeNullable(username);
        return friendshipRepository.findAll().stream()
            .filter(friendship -> !StringUtils.hasText(normalized)
                || friendship.getRequesterUser().getUsername().toLowerCase().contains(normalized.toLowerCase())
                || friendship.getAddresseeUser().getUsername().toLowerCase().contains(normalized.toLowerCase()))
            .map(this::toAdminDirectConversation)
            .toList();
    }

    @Transactional
    public SupportConversation assignConversation(String conversationId, String agentUsername) {
        requireAdmin();
        SupportConversationEntity conversation = supportConversationRepository.findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Support conversation not found"));
        UserEntity agent = userRepository.findByUsername(agentUsername.trim())
            .orElseThrow(() -> new IllegalArgumentException("Agent not found"));
        if (!"AGENT".equalsIgnoreCase(agent.getRoleCode()) || !"ACTIVE".equalsIgnoreCase(agent.getStatusCode())) {
            throw new IllegalArgumentException("Target agent is not active");
        }

        conversation.setAssignedAgent(agent);
        conversation.setAssignmentStatus("MANUALLY_ASSIGNED");
        conversation.setUpdatedAt(LocalDateTime.now());
        return persistentSupportService.toSupportConversation(supportConversationRepository.save(conversation));
    }

    private void requireAdmin() {
        currentUserService.requireAdmin(currentUserService.getCurrentUser());
    }

    private AdminDirectConversation toAdminDirectConversation(FriendshipEntity friendship) {
        return new AdminDirectConversation(
            friendship.getId(),
            friendship.getRequesterUser().getUsername(),
            friendship.getAddresseeUser().getUsername(),
            friendship.getStatusCode(),
            directMessageRepository.findByFriendship_IdOrderByCreatedAtAsc(friendship.getId()).stream()
                .map(this::toAdminChatMessage)
                .toList()
        );
    }

    private ChatMessage toAdminChatMessage(DirectMessageEntity message) {
        return new ChatMessage(
            message.getId(),
            message.getSenderUser().getUsername(),
            message.getMessageType().toLowerCase(),
            message.getContent(),
            MESSAGE_TIME_FORMATTER.format(message.getCreatedAt()),
            "none"
        );
    }

    private String normalizeStatus(String status) {
        String normalized = requireTrimmed(status, "Status is required").toUpperCase();
        if (!List.of("ACTIVE", "DISABLED", "BLOCKED").contains(normalized)) {
            throw new IllegalArgumentException("Unsupported user status");
        }
        return normalized;
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

    private String value(String value) {
        return value == null ? "" : value;
    }
}
