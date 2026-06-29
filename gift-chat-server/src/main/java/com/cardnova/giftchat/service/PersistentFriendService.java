package com.cardnova.giftchat.service;

import com.cardnova.giftchat.dto.CreateFriendRequest;
import com.cardnova.giftchat.dto.SearchFriendResponse;
import com.cardnova.giftchat.dto.SendDirectMessageRequest;
import com.cardnova.giftchat.entity.DirectMessageEntity;
import com.cardnova.giftchat.entity.FriendshipEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.model.ChatMessage;
import com.cardnova.giftchat.model.FriendProfile;
import com.cardnova.giftchat.model.FriendRequestItem;
import com.cardnova.giftchat.repository.DirectMessageRepository;
import com.cardnova.giftchat.repository.FriendshipRepository;
import com.cardnova.giftchat.repository.BlacklistEntryRepository;
import com.cardnova.giftchat.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PersistentFriendService {

    private static final DateTimeFormatter MESSAGE_TIME_FORMATTER = DateTimeFormatter.ofPattern("MM-dd HH:mm");
    private static final DateTimeFormatter REQUEST_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final List<String> FRIEND_TAGS = List.of("Verified friend", "Supports image and voice");
    private static final Set<String> ACTIVE_RELATIONSHIP_STATES = Set.of("PENDING", "ACCEPTED", "BLOCKED");

    private final FriendshipRepository friendshipRepository;
    private final DirectMessageRepository directMessageRepository;
    private final BlacklistEntryRepository blacklistEntryRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final ConversationReadService conversationReadService;
    private final RealtimeChatService realtimeChatService;

    public PersistentFriendService(
        FriendshipRepository friendshipRepository,
        DirectMessageRepository directMessageRepository,
        BlacklistEntryRepository blacklistEntryRepository,
        UserRepository userRepository,
        CurrentUserService currentUserService,
        ConversationReadService conversationReadService,
        RealtimeChatService realtimeChatService
    ) {
        this.friendshipRepository = friendshipRepository;
        this.directMessageRepository = directMessageRepository;
        this.blacklistEntryRepository = blacklistEntryRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.conversationReadService = conversationReadService;
        this.realtimeChatService = realtimeChatService;
    }

    public List<FriendProfile> getFriends() {
        UserEntity currentUser = currentUserService.getCurrentUser();

        return friendshipRepository.findByRequesterUser_IdOrAddresseeUser_IdOrderByUpdatedAtDesc(currentUser.getId(), currentUser.getId()).stream()
            .filter(friendship -> "ACCEPTED".equals(friendship.getStatusCode()))
            .map(friendship -> toFriendProfile(friendship, currentUser.getId()))
            .toList();
    }

    @Transactional
    public FriendProfile markConversationRead(String friendshipId) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        FriendshipEntity friendship = friendshipRepository.findById(friendshipId)
            .orElseThrow(() -> new IllegalArgumentException("Friendship not found"));
        if (!isParticipant(friendship, currentUser.getId()) || !"ACCEPTED".equals(friendship.getStatusCode())) {
            throw new IllegalArgumentException("Friendship not accessible");
        }

        LocalDateTime latestMessageAt = directMessageRepository.findByFriendship_IdOrderByCreatedAtAsc(friendshipId).stream()
            .map(DirectMessageEntity::getCreatedAt)
            .max(LocalDateTime::compareTo)
            .orElse(null);
        LocalDateTime readAt = conversationReadService.markRead("friend", friendshipId, currentUser, latestMessageAt);
        realtimeChatService.broadcastReadReceipt(
            RealtimeChatService.friendChannel(friendshipId),
            "friend",
            friendshipId,
            currentUser.getId(),
            currentUser.getUsername(),
            MESSAGE_TIME_FORMATTER.format(readAt)
        );
        return toFriendProfile(friendship, currentUser.getId());
    }

    public List<SearchFriendResponse> searchUsers(String keyword) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        String normalized = keyword == null ? "" : keyword.trim().toLowerCase();
        if (normalized.isEmpty()) {
            return List.of();
        }

        return userRepository.findAll().stream()
            .filter(user -> !currentUser.getUsername().equalsIgnoreCase(user.getUsername()))
            .filter(user -> user.getUsername().toLowerCase().contains(normalized))
            .map(user -> new SearchFriendResponse(user.getUsername(), user.getUsername(), relationshipStatus(currentUser, user)))
            .toList();
    }

    public List<FriendRequestItem> getFriendRequests() {
        UserEntity currentUser = currentUserService.getCurrentUser();

        List<FriendRequestItem> incoming = friendshipRepository.findByAddresseeUser_IdAndStatusCodeOrderByCreatedAtDesc(currentUser.getId(), "PENDING").stream()
            .map(friendship -> new FriendRequestItem(
                friendship.getId(),
                friendship.getRequesterUser().getUsername(),
                friendship.getRequesterUser().getUsername(),
                "incoming",
                "pending",
                REQUEST_TIME_FORMATTER.format(friendship.getCreatedAt())
            ))
            .toList();

        List<FriendRequestItem> outgoing = friendshipRepository.findByRequesterUser_IdAndStatusCodeOrderByCreatedAtDesc(currentUser.getId(), "PENDING").stream()
            .map(friendship -> new FriendRequestItem(
                friendship.getId(),
                friendship.getAddresseeUser().getUsername(),
                friendship.getAddresseeUser().getUsername(),
                "outgoing",
                "pending",
                REQUEST_TIME_FORMATTER.format(friendship.getCreatedAt())
            ))
            .toList();

        return java.util.stream.Stream.concat(incoming.stream(), outgoing.stream()).toList();
    }

    @Transactional
    public FriendRequestItem createFriendRequest(CreateFriendRequest request) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        String targetUsername = request.username().trim();
        UserEntity targetUser = userRepository.findByUsername(targetUsername)
            .orElseThrow(() -> new IllegalArgumentException("Target user not found"));

        if (currentUser.getId().equals(targetUser.getId())) {
            throw new IllegalArgumentException("Cannot add yourself");
        }
        if (hasBlacklistRelationship(currentUser.getId(), targetUser.getId())) {
            throw new IllegalArgumentException("Friend request blocked by blacklist relationship");
        }

        FriendshipEntity existingForward = friendshipRepository.findByRequesterUser_IdAndAddresseeUser_Id(currentUser.getId(), targetUser.getId())
            .orElse(null);
        if (existingForward != null && ACTIVE_RELATIONSHIP_STATES.contains(existingForward.getStatusCode())) {
            throw new IllegalArgumentException("Friend relationship already exists");
        }

        FriendshipEntity existingReverse = friendshipRepository.findByAddresseeUser_IdAndRequesterUser_Id(currentUser.getId(), targetUser.getId())
            .orElse(null);
        if (existingReverse != null && "PENDING".equals(existingReverse.getStatusCode())) {
            existingReverse.setStatusCode("ACCEPTED");
            existingReverse.setUpdatedAt(LocalDateTime.now());
            FriendshipEntity accepted = friendshipRepository.save(existingReverse);
            return new FriendRequestItem(
                accepted.getId(),
                targetUser.getUsername(),
                targetUser.getUsername(),
                "incoming",
                "accepted",
                REQUEST_TIME_FORMATTER.format(accepted.getUpdatedAt())
            );
        }

        FriendshipEntity friendship = new FriendshipEntity();
        friendship.setId(UUID.randomUUID().toString());
        friendship.setRequesterUser(currentUser);
        friendship.setAddresseeUser(targetUser);
        friendship.setStatusCode("PENDING");
        friendship.setCreatedAt(LocalDateTime.now());
        friendship.setUpdatedAt(LocalDateTime.now());
        FriendshipEntity saved = friendshipRepository.save(friendship);

        return new FriendRequestItem(
            saved.getId(),
            targetUser.getUsername(),
            targetUser.getUsername(),
            "outgoing",
            "pending",
            REQUEST_TIME_FORMATTER.format(saved.getCreatedAt())
        );
    }

    @Transactional
    public FriendRequestItem acceptRequest(String friendshipId) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        FriendshipEntity friendship = friendshipRepository.findById(friendshipId)
            .orElseThrow(() -> new IllegalArgumentException("Friend request not found"));

        if (!friendship.getAddresseeUser().getId().equals(currentUser.getId()) || !"PENDING".equals(friendship.getStatusCode())) {
            throw new IllegalArgumentException("Friend request cannot be accepted");
        }

        friendship.setStatusCode("ACCEPTED");
        friendship.setUpdatedAt(LocalDateTime.now());
        FriendshipEntity saved = friendshipRepository.save(friendship);
        return new FriendRequestItem(
            saved.getId(),
            saved.getRequesterUser().getUsername(),
            saved.getRequesterUser().getUsername(),
            "incoming",
            "accepted",
            REQUEST_TIME_FORMATTER.format(saved.getUpdatedAt())
        );
    }

    @Transactional
    public FriendRequestItem rejectRequest(String friendshipId) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        FriendshipEntity friendship = friendshipRepository.findById(friendshipId)
            .orElseThrow(() -> new IllegalArgumentException("Friend request not found"));

        if (!friendship.getAddresseeUser().getId().equals(currentUser.getId()) || !"PENDING".equals(friendship.getStatusCode())) {
            throw new IllegalArgumentException("Friend request cannot be rejected");
        }

        friendship.setStatusCode("REJECTED");
        friendship.setUpdatedAt(LocalDateTime.now());
        FriendshipEntity saved = friendshipRepository.save(friendship);
        return new FriendRequestItem(
            saved.getId(),
            saved.getRequesterUser().getUsername(),
            saved.getRequesterUser().getUsername(),
            "incoming",
            "rejected",
            REQUEST_TIME_FORMATTER.format(saved.getUpdatedAt())
        );
    }

    @Transactional
    public FriendProfile removeFriend(String friendshipId) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        FriendshipEntity friendship = friendshipRepository.findById(friendshipId)
            .orElseThrow(() -> new IllegalArgumentException("Friendship not found"));

        if (!isParticipant(friendship, currentUser.getId()) || !"ACCEPTED".equals(friendship.getStatusCode())) {
            throw new IllegalArgumentException("Friendship cannot be removed");
        }

        FriendProfile removed = toFriendProfile(friendship, currentUser.getId());
        friendship.setStatusCode("REMOVED");
        friendship.setUpdatedAt(LocalDateTime.now());
        friendshipRepository.save(friendship);
        return removed;
    }

    @Transactional
    public ChatMessage sendMessage(String friendshipId, SendDirectMessageRequest request) {
        UserEntity currentUser = currentUserService.getCurrentUser();

        FriendshipEntity friendship = friendshipRepository.findById(friendshipId)
            .orElseThrow(() -> new IllegalArgumentException("Friendship not found"));

        if (!isParticipant(friendship, currentUser.getId()) || !"ACCEPTED".equals(friendship.getStatusCode())) {
            throw new IllegalArgumentException("Friendship not accessible");
        }
        UserEntity targetUser = friendship.getRequesterUser().getId().equals(currentUser.getId())
            ? friendship.getAddresseeUser()
            : friendship.getRequesterUser();
        if (hasBlacklistRelationship(currentUser.getId(), targetUser.getId())) {
            throw new IllegalArgumentException("Direct message blocked by blacklist relationship");
        }

        DirectMessageEntity entity = new DirectMessageEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setFriendship(friendship);
        entity.setSenderUser(currentUser);
        entity.setMessageType(request.messageType().trim().toUpperCase());
        entity.setContent(request.content().trim());
        entity.setCreatedAt(LocalDateTime.now());
        DirectMessageEntity saved = directMessageRepository.save(entity);

        friendship.setUpdatedAt(LocalDateTime.now());
        friendshipRepository.save(friendship);

        ChatMessage message = new ChatMessage(
            saved.getId(),
            "me",
            saved.getMessageType().toLowerCase(),
            saved.getContent(),
            MESSAGE_TIME_FORMATTER.format(saved.getCreatedAt()),
            "sent"
        );
        realtimeChatService.broadcast(
            RealtimeChatService.friendChannel(friendshipId),
            currentUser.getId(),
            "me",
            "friend",
            saved.getMessageType().toLowerCase(),
            saved.getContent(),
            saved.getId(),
            MESSAGE_TIME_FORMATTER.format(saved.getCreatedAt())
        );
        return message;
    }

    private FriendProfile toFriendProfile(FriendshipEntity friendship, String currentUserId) {
        UserEntity counterpart = friendship.getRequesterUser().getId().equals(currentUserId)
            ? friendship.getAddresseeUser()
            : friendship.getRequesterUser();

        List<DirectMessageEntity> messages = directMessageRepository.findByFriendship_IdOrderByCreatedAtAsc(friendship.getId());
        LocalDateTime lastReadAt = conversationReadService.getLastReadAt("friend", friendship.getId(), currentUserId);
        LocalDateTime counterpartReadAt = conversationReadService.getReadAt("friend", friendship.getId(), counterpart.getId());

        return new FriendProfile(
            friendship.getId(),
            counterpart.getUsername(),
            counterpart.getUsername(),
            counterpart.getPhone() == null ? "" : counterpart.getPhone(),
            "ACTIVE".equals(counterpart.getStatusCode()) ? "online" : "offline",
            FRIEND_TAGS,
            messages.stream()
                .map(message -> new ChatMessage(
                    message.getId(),
                    message.getSenderUser().getId().equals(currentUserId) ? "me" : "friend",
                    message.getMessageType().toLowerCase(),
                    message.getContent(),
                    MESSAGE_TIME_FORMATTER.format(message.getCreatedAt()),
                    message.getSenderUser().getId().equals(currentUserId)
                        ? ((counterpartReadAt != null && !message.getCreatedAt().isAfter(counterpartReadAt)) ? "read" : "sent")
                        : "none"
                ))
                .toList(),
            (int) messages.stream()
                .filter(message -> !message.getSenderUser().getId().equals(currentUserId))
                .filter(message -> lastReadAt == null || message.getCreatedAt().isAfter(lastReadAt))
                .count(),
            ""
        );
    }

    private boolean isParticipant(FriendshipEntity friendship, String currentUserId) {
        return friendship.getRequesterUser().getId().equals(currentUserId)
            || friendship.getAddresseeUser().getId().equals(currentUserId);
    }

    private String relationshipStatus(UserEntity currentUser, UserEntity targetUser) {
        if (hasBlacklistRelationship(currentUser.getId(), targetUser.getId())) {
            return "blocked";
        }

        FriendshipEntity forward = friendshipRepository.findByRequesterUser_IdAndAddresseeUser_Id(currentUser.getId(), targetUser.getId()).orElse(null);
        if (forward != null) {
            return switch (forward.getStatusCode()) {
                case "ACCEPTED" -> "accepted";
                case "PENDING" -> "pending_outgoing";
                case "BLOCKED" -> "blocked";
                default -> "searchable";
            };
        }

        FriendshipEntity reverse = friendshipRepository.findByAddresseeUser_IdAndRequesterUser_Id(currentUser.getId(), targetUser.getId()).orElse(null);
        if (reverse != null) {
            return switch (reverse.getStatusCode()) {
                case "ACCEPTED" -> "accepted";
                case "PENDING" -> "pending_incoming";
                case "BLOCKED" -> "blocked";
                default -> "searchable";
            };
        }

        return "searchable";
    }

    private boolean hasBlacklistRelationship(String currentUserId, String targetUserId) {
        return blacklistEntryRepository.existsByOwnerUser_IdAndBlockedUser_Id(currentUserId, targetUserId)
            || blacklistEntryRepository.existsByOwnerUser_IdAndBlockedUser_Id(targetUserId, currentUserId);
    }
}
