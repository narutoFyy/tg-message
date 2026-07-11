package com.cardnova.giftchat.service;

import com.cardnova.giftchat.dto.CreateAgentRequest;
import com.cardnova.giftchat.dto.UpdateAgentWelcomeMessageRequest;
import com.cardnova.giftchat.entity.AgentWelcomeMessageEntity;
import com.cardnova.giftchat.entity.DirectMessageEntity;
import com.cardnova.giftchat.entity.FriendshipEntity;
import com.cardnova.giftchat.entity.SupportConversationEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.model.AdminDirectConversation;
import com.cardnova.giftchat.model.AdminUserItem;
import com.cardnova.giftchat.model.AgentItem;
import com.cardnova.giftchat.model.ChatMessage;
import com.cardnova.giftchat.model.SupportConversation;
import com.cardnova.giftchat.model.VipSummary;
import com.cardnova.giftchat.repository.BlacklistEntryRepository;
import com.cardnova.giftchat.repository.AgentWelcomeMessageRepository;
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
    private final AgentWelcomeMessageRepository agentWelcomeMessageRepository;
    private final BlacklistEntryRepository blacklistEntryRepository;
    private final FriendshipRepository friendshipRepository;
    private final DirectMessageRepository directMessageRepository;
    private final SupportConversationRepository supportConversationRepository;
    private final PersistentSupportService persistentSupportService;
    private final CurrentUserService currentUserService;
    private final PasswordService passwordService;
    private final VipService vipService;

    public AdminService(
        UserRepository userRepository,
        AgentWelcomeMessageRepository agentWelcomeMessageRepository,
        BlacklistEntryRepository blacklistEntryRepository,
        FriendshipRepository friendshipRepository,
        DirectMessageRepository directMessageRepository,
        SupportConversationRepository supportConversationRepository,
        PersistentSupportService persistentSupportService,
        CurrentUserService currentUserService,
        PasswordService passwordService,
        VipService vipService
    ) {
        this.userRepository = userRepository;
        this.agentWelcomeMessageRepository = agentWelcomeMessageRepository;
        this.blacklistEntryRepository = blacklistEntryRepository;
        this.friendshipRepository = friendshipRepository;
        this.directMessageRepository = directMessageRepository;
        this.supportConversationRepository = supportConversationRepository;
        this.persistentSupportService = persistentSupportService;
        this.currentUserService = currentUserService;
        this.passwordService = passwordService;
        this.vipService = vipService;
    }

    public List<AdminUserItem> users() {
        requireAdmin();
        return userRepository.findAll().stream()
            .map(this::toAdminUserItem)
            .toList();
    }

    public List<AgentItem> agents() {
        requireAdmin();
        return userRepository.findByRoleCodeOrderByCreatedAtDesc("AGENT").stream()
            .map(this::toAgentItem)
            .toList();
    }

    @Transactional
    public AgentItem createAgent(CreateAgentRequest request) {
        UserEntity admin = requireAdmin();
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

        LocalDateTime now = LocalDateTime.now();
        AgentWelcomeMessageEntity welcome = new AgentWelcomeMessageEntity();
        welcome.setId(UUID.randomUUID().toString());
        welcome.setAgent(saved);
        welcome.setContent(AgentWelcomeMessageDefaults.CONTENT);
        welcome.setEnabled(true);
        welcome.setUpdatedBy(admin);
        welcome.setCreatedAt(now);
        welcome.setUpdatedAt(now);
        agentWelcomeMessageRepository.save(welcome);

        return toAgentItem(saved);
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
        return toAgentItem(saved);
    }

    @Transactional
    public AgentItem updateAgentWelcomeMessage(String agentId, UpdateAgentWelcomeMessageRequest request) {
        UserEntity admin = requireAdmin();
        UserEntity agent = userRepository.findById(agentId)
            .orElseThrow(() -> new IllegalArgumentException("Agent not found"));
        if (!"AGENT".equalsIgnoreCase(agent.getRoleCode())) {
            throw new IllegalArgumentException("User is not an agent");
        }

        boolean enabled = Boolean.TRUE.equals(request.enabled());
        String content = normalizeWelcomeContent(request.content());
        if (enabled && !StringUtils.hasText(content)) {
            throw new IllegalArgumentException("Welcome message content is required when enabled");
        }

        LocalDateTime now = LocalDateTime.now();
        AgentWelcomeMessageEntity welcome = agentWelcomeMessageRepository.findByAgent_Id(agent.getId())
            .orElseGet(() -> {
                AgentWelcomeMessageEntity created = new AgentWelcomeMessageEntity();
                created.setId(UUID.randomUUID().toString());
                created.setAgent(agent);
                created.setCreatedAt(now);
                return created;
            });
        welcome.setContent(content);
        welcome.setEnabled(enabled);
        welcome.setUpdatedBy(admin);
        welcome.setUpdatedAt(now);
        agentWelcomeMessageRepository.save(welcome);
        return toAgentItem(agent);
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

    private UserEntity requireAdmin() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        currentUserService.requireAdmin(currentUser);
        return currentUser;
    }

    private AgentItem toAgentItem(UserEntity agent) {
        AgentWelcomeMessageEntity welcome = agentWelcomeMessageRepository.findByAgent_Id(agent.getId()).orElse(null);
        return new AgentItem(
            agent.getId(),
            agent.getUsername(),
            value(agent.getEmail()),
            value(agent.getPhone()),
            agent.getStatusCode(),
            supportConversationRepository.countByAssignedAgent_Id(agent.getId()),
            welcome == null ? "" : value(welcome.getContent()),
            welcome != null && Boolean.TRUE.equals(welcome.getEnabled()),
            welcome == null ? "" : USER_TIME_FORMATTER.format(welcome.getUpdatedAt()),
            welcome == null || welcome.getUpdatedBy() == null ? "" : welcome.getUpdatedBy().getUsername()
        );
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

    private AdminUserItem toAdminUserItem(UserEntity user) {
        VipSummary vip = vipService.summaryForUser(user.getId());
        return new AdminUserItem(
            user.getId(),
            user.getUsername(),
            user.getEmail() == null ? "" : user.getEmail(),
            user.getPhone() == null ? "" : user.getPhone(),
            user.getRoleCode(),
            user.getStatusCode(),
            blacklistEntryRepository.existsByBlockedUser_Id(user.getId()),
            vip.level(),
            vip.points(),
            USER_TIME_FORMATTER.format(user.getCreatedAt())
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

    private String normalizeWelcomeContent(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.length() > 1000) {
            throw new IllegalArgumentException("Welcome message must be 1000 characters or less");
        }
        return normalized;
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
