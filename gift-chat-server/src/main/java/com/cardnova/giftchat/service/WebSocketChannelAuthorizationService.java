package com.cardnova.giftchat.service;

import com.cardnova.giftchat.api.ForbiddenException;
import com.cardnova.giftchat.api.UnauthorizedException;
import com.cardnova.giftchat.entity.FriendshipEntity;
import com.cardnova.giftchat.entity.SupportConversationEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.repository.FriendshipRepository;
import com.cardnova.giftchat.repository.SupportConversationRepository;
import com.cardnova.giftchat.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class WebSocketChannelAuthorizationService {

    private final UserRepository userRepository;
    private final SupportConversationRepository supportConversationRepository;
    private final FriendshipRepository friendshipRepository;

    public WebSocketChannelAuthorizationService(
        UserRepository userRepository,
        SupportConversationRepository supportConversationRepository,
        FriendshipRepository friendshipRepository
    ) {
        this.userRepository = userRepository;
        this.supportConversationRepository = supportConversationRepository;
        this.friendshipRepository = friendshipRepository;
    }

    public UserEntity requireAccess(String userId, String channelType, String channelId) {
        if (!StringUtils.hasText(userId)) {
            throw new UnauthorizedException("Missing websocket user");
        }
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new UnauthorizedException("Token user not found"));
        if (!"ACTIVE".equalsIgnoreCase(user.getStatusCode())) {
            throw new ForbiddenException("User is not active");
        }

        String normalizedType = channelType == null ? "" : channelType.trim().toLowerCase();
        if ("support".equals(normalizedType)) {
            requireSupportAccess(user, channelId);
            return user;
        }
        if ("friend".equals(normalizedType)) {
            requireFriendAccess(user, channelId);
            return user;
        }
        throw new ForbiddenException("Unsupported websocket channel");
    }

    private void requireSupportAccess(UserEntity user, String conversationId) {
        SupportConversationEntity conversation = supportConversationRepository.findById(conversationId)
            .orElseThrow(() -> new ForbiddenException("Support conversation not accessible"));
        if ("ADMIN".equalsIgnoreCase(user.getRoleCode())) {
            return;
        }
        if ("USER".equalsIgnoreCase(user.getRoleCode())
            && conversation.getCustomerUser().getId().equals(user.getId())) {
            return;
        }
        if ("AGENT".equalsIgnoreCase(user.getRoleCode())
            && conversation.getAssignedAgent() != null
            && conversation.getAssignedAgent().getId().equals(user.getId())) {
            return;
        }
        throw new ForbiddenException("Support conversation not accessible");
    }

    private void requireFriendAccess(UserEntity user, String friendshipId) {
        FriendshipEntity friendship = friendshipRepository.findById(friendshipId)
            .orElseThrow(() -> new ForbiddenException("Friend conversation not accessible"));
        if (!"ACCEPTED".equalsIgnoreCase(friendship.getStatusCode())) {
            throw new ForbiddenException("Friend conversation not active");
        }
        if (friendship.getRequesterUser().getId().equals(user.getId())
            || friendship.getAddresseeUser().getId().equals(user.getId())) {
            return;
        }
        throw new ForbiddenException("Friend conversation not accessible");
    }
}
