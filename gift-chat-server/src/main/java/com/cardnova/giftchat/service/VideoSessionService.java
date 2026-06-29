package com.cardnova.giftchat.service;

import com.cardnova.giftchat.api.ForbiddenException;
import com.cardnova.giftchat.dto.CreateVideoSessionRequest;
import com.cardnova.giftchat.entity.DirectMessageEntity;
import com.cardnova.giftchat.entity.FriendshipEntity;
import com.cardnova.giftchat.entity.SupportConversationEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.entity.VideoSessionEntity;
import com.cardnova.giftchat.model.VideoSessionBootstrap;
import com.cardnova.giftchat.model.VideoSessionItem;
import com.cardnova.giftchat.repository.DirectMessageRepository;
import com.cardnova.giftchat.repository.FriendshipRepository;
import com.cardnova.giftchat.repository.SupportConversationRepository;
import com.cardnova.giftchat.repository.VideoSessionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class VideoSessionService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Set<String> ALLOWED_STATUSES = Set.of("created", "joining", "active", "ended", "missed", "rejected");

    private final VideoSessionRepository videoSessionRepository;
    private final FriendshipRepository friendshipRepository;
    private final DirectMessageRepository directMessageRepository;
    private final SupportConversationRepository supportConversationRepository;
    private final CurrentUserService currentUserService;
    private final PersistentSupportService persistentSupportService;
    private final NotificationService notificationService;
    private final RealtimeChatService realtimeChatService;
    private final TrtcUserSigService trtcUserSigService;
    private final ObjectMapper objectMapper;
    private final String trtcSdkAppId;

    public VideoSessionService(
        VideoSessionRepository videoSessionRepository,
        FriendshipRepository friendshipRepository,
        DirectMessageRepository directMessageRepository,
        SupportConversationRepository supportConversationRepository,
        CurrentUserService currentUserService,
        PersistentSupportService persistentSupportService,
        NotificationService notificationService,
        RealtimeChatService realtimeChatService,
        TrtcUserSigService trtcUserSigService,
        ObjectMapper objectMapper,
        @Value("${app.video.trtc-sdk-app-id:}") String trtcSdkAppId
    ) {
        this.videoSessionRepository = videoSessionRepository;
        this.friendshipRepository = friendshipRepository;
        this.directMessageRepository = directMessageRepository;
        this.supportConversationRepository = supportConversationRepository;
        this.currentUserService = currentUserService;
        this.persistentSupportService = persistentSupportService;
        this.notificationService = notificationService;
        this.realtimeChatService = realtimeChatService;
        this.trtcUserSigService = trtcUserSigService;
        this.objectMapper = objectMapper;
        this.trtcSdkAppId = trtcSdkAppId;
    }

    public List<VideoSessionItem> getVideoSessions() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        if ("ADMIN".equalsIgnoreCase(currentUser.getRoleCode())) {
            return videoSessionRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toItem).toList();
        }
        return videoSessionRepository.findByInitiatorUser_IdOrReceiverUser_IdOrderByCreatedAtDesc(currentUser.getId(), currentUser.getId())
            .stream()
            .map(this::toItem)
            .toList();
    }

    public VideoSessionBootstrap getBootstrap(String sessionId) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        VideoSessionEntity entity = videoSessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Video session not found"));
        ensureParticipantOrAdmin(currentUser, entity);
        return toBootstrap(entity, currentUser);
    }

    @Transactional
    public VideoSessionBootstrap create(CreateVideoSessionRequest request) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        String channelType = request.channelType().trim().toLowerCase();
        String channelId = request.channelId().trim();

        VideoSessionEntity entity = switch (channelType) {
            case "support" -> createSupportSession(currentUser, channelId);
            case "friend" -> createFriendSession(currentUser, channelId);
            default -> throw new IllegalArgumentException("Unsupported video channel type");
        };

        VideoSessionEntity saved = videoSessionRepository.save(entity);
        broadcastVideoInvite(saved);
        return toBootstrap(saved, currentUser);
    }

    private void broadcastVideoInvite(VideoSessionEntity entity) {
        String channelType = entity.getChannelType().toLowerCase();
        String channelKey = switch (channelType) {
            case "support" -> RealtimeChatService.supportChannel(entity.getChannelId());
            case "friend" -> RealtimeChatService.friendChannel(entity.getChannelId());
            default -> "";
        };
        if (channelKey.isBlank()) {
            return;
        }

        realtimeChatService.broadcastVideoInvite(
            channelKey,
            channelType,
            entity.getChannelId(),
            entity.getId(),
            entity.getRoomId(),
            entity.getInitiatorUser().getUsername(),
            entity.getReceiverUser().getUsername(),
            TIME_FORMATTER.format(entity.getCreatedAt())
        );
    }

    private VideoSessionBootstrap toBootstrap(VideoSessionEntity entity, UserEntity currentUser) {
        String trtcUserId = buildTrtcUserId(currentUser);
        boolean sdkConfigured = !trtcSdkAppId.isBlank() && trtcUserSigService.isConfigured();
        String userSig = sdkConfigured ? trtcUserSigService.generate(trtcUserId) : "";
        String note = sdkConfigured
            ? "TRTC session ready."
            : "TRTC SDKAppID or SecretKey is not configured.";
        return new VideoSessionBootstrap(
            toItem(entity),
            trtcSdkAppId,
            trtcUserId,
            userSig,
            sdkConfigured,
            "trtc",
            note
        );
    }

    @Transactional
    public VideoSessionItem updateStatus(String sessionId, String status) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        VideoSessionEntity entity = videoSessionRepository.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Video session not found"));
        ensureParticipantOrAdmin(currentUser, entity);

        String normalized = status.trim().toLowerCase();
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported video session status");
        }

        entity.setStatusCode(normalized.toUpperCase());
        entity.setUpdatedAt(LocalDateTime.now());
        if ("active".equals(normalized) && entity.getStartedAt() == null) {
            entity.setStartedAt(LocalDateTime.now());
        }
        if (List.of("ended", "missed", "rejected").contains(normalized)) {
            entity.setEndedAt(LocalDateTime.now());
        }
        return toItem(videoSessionRepository.save(entity));
    }

    private VideoSessionEntity createSupportSession(UserEntity currentUser, String conversationId) {
        SupportConversationEntity conversation = supportConversationRepository.findById(conversationId)
            .orElseThrow(() -> new IllegalArgumentException("Support conversation not found"));

        UserEntity receiver;
        if ("AGENT".equalsIgnoreCase(currentUser.getRoleCode())) {
            if (conversation.getAssignedAgent() == null || !conversation.getAssignedAgent().getId().equals(currentUser.getId())) {
                throw new ForbiddenException("Support conversation not accessible");
            }
            receiver = conversation.getCustomerUser();
        } else if ("USER".equalsIgnoreCase(currentUser.getRoleCode())) {
            if (!conversation.getCustomerUser().getId().equals(currentUser.getId())) {
                throw new ForbiddenException("Support conversation not accessible");
            }
            receiver = conversation.getAssignedAgent();
            if (receiver == null) {
                throw new IllegalArgumentException("No assigned support agent available");
            }
        } else {
            throw new ForbiddenException("Support video is only for user and agent");
        }

        VideoSessionEntity entity = baseSession("support", conversationId, currentUser, receiver);
        persistentSupportService.appendStaffMessage(
            conversation,
            currentUser,
            "AGENT".equalsIgnoreCase(currentUser.getRoleCode()) ? "SUPPORT" : "ME",
            "VIDEO",
            videoInviteContent(entity)
        );
        notificationService.notifyUser(
            receiver,
            currentUser,
            "VIDEO_CALL",
            "New video call request",
            currentUser.getUsername() + " invited you to a video call.",
            "VIDEO_SESSION",
            entity.getId()
        );
        return entity;
    }

    private VideoSessionEntity createFriendSession(UserEntity currentUser, String friendshipId) {
        FriendshipEntity friendship = friendshipRepository.findById(friendshipId)
            .orElseThrow(() -> new IllegalArgumentException("Friendship not found"));
        if (!"ACCEPTED".equalsIgnoreCase(friendship.getStatusCode())) {
            throw new IllegalArgumentException("Friendship is not active");
        }

        UserEntity receiver;
        if (friendship.getRequesterUser().getId().equals(currentUser.getId())) {
            receiver = friendship.getAddresseeUser();
        } else if (friendship.getAddresseeUser().getId().equals(currentUser.getId())) {
            receiver = friendship.getRequesterUser();
        } else {
            throw new ForbiddenException("Friendship not accessible");
        }

        VideoSessionEntity entity = baseSession("friend", friendshipId, currentUser, receiver);

        DirectMessageEntity message = new DirectMessageEntity();
        message.setId(UUID.randomUUID().toString());
        message.setFriendship(friendship);
        message.setSenderUser(currentUser);
        message.setMessageType("TEXT");
        message.setContent("Video call request created. Room " + entity.getRoomId());
        message.setCreatedAt(LocalDateTime.now());
        directMessageRepository.save(message);

        notificationService.notifyUser(
            receiver,
            currentUser,
            "VIDEO_CALL",
            "New video call request",
            currentUser.getUsername() + " invited you to a video call.",
            "VIDEO_SESSION",
            entity.getId()
        );
        return entity;
    }

    private VideoSessionEntity baseSession(String channelType, String channelId, UserEntity initiator, UserEntity receiver) {
        VideoSessionEntity entity = new VideoSessionEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setRoomId("video-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        entity.setChannelType(channelType.toUpperCase());
        entity.setChannelId(channelId);
        entity.setInitiatorUser(initiator);
        entity.setReceiverUser(receiver);
        entity.setStatusCode("CREATED");
        entity.setVendorCode("TRTC");
        entity.setStartedAt(null);
        entity.setEndedAt(null);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }

    private String videoInviteContent(VideoSessionEntity entity) {
        Map<String, String> payload = Map.of(
            "kind", "video_call",
            "sessionId", entity.getId(),
            "roomId", entity.getRoomId(),
            "channelType", entity.getChannelType().toLowerCase(),
            "channelId", entity.getChannelId(),
            "initiatorUsername", entity.getInitiatorUser().getUsername(),
            "receiverUsername", entity.getReceiverUser().getUsername()
        );
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            return "Video call request created. Room " + entity.getRoomId();
        }
    }

    private String buildTrtcUserId(UserEntity user) {
        String username = user.getUsername() == null ? "user" : user.getUsername();
        String safeUsername = username.replaceAll("[^A-Za-z0-9_-]", "_");
        String userId = "u_" + user.getId().replaceAll("[^A-Za-z0-9_-]", "_") + "_" + safeUsername;
        return userId.length() <= 32 ? userId : userId.substring(0, 32);
    }

    private void ensureParticipantOrAdmin(UserEntity currentUser, VideoSessionEntity entity) {
        if ("ADMIN".equalsIgnoreCase(currentUser.getRoleCode())) {
            return;
        }
        if (!entity.getInitiatorUser().getId().equals(currentUser.getId())
            && !entity.getReceiverUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("Video session not accessible");
        }
    }

    private VideoSessionItem toItem(VideoSessionEntity entity) {
        return new VideoSessionItem(
            entity.getId(),
            entity.getRoomId(),
            entity.getChannelType().toLowerCase(),
            entity.getChannelId(),
            entity.getInitiatorUser().getUsername(),
            entity.getReceiverUser().getUsername(),
            entity.getVendorCode().toLowerCase(),
            entity.getStatusCode().toLowerCase(),
            entity.getStartedAt() == null ? "" : TIME_FORMATTER.format(entity.getStartedAt()),
            entity.getEndedAt() == null ? "" : TIME_FORMATTER.format(entity.getEndedAt()),
            TIME_FORMATTER.format(entity.getCreatedAt()),
            TIME_FORMATTER.format(entity.getUpdatedAt())
        );
    }
}
