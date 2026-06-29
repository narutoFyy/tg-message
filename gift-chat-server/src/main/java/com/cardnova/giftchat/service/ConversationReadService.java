package com.cardnova.giftchat.service;

import com.cardnova.giftchat.entity.ConversationReadStateEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.repository.ConversationReadStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ConversationReadService {

    private final ConversationReadStateRepository conversationReadStateRepository;

    public ConversationReadService(ConversationReadStateRepository conversationReadStateRepository) {
        this.conversationReadStateRepository = conversationReadStateRepository;
    }

    public LocalDateTime getLastReadAt(String conversationType, String conversationId, String userId) {
        return conversationReadStateRepository
            .findByConversationTypeAndConversationIdAndUser_Id(conversationType, conversationId, userId)
            .map(ConversationReadStateEntity::getLastReadAt)
            .orElse(null);
    }

    public LocalDateTime getCounterpartReadAt(String conversationType, String conversationId, String currentUserId) {
        return conversationReadStateRepository
            .findByConversationTypeAndConversationIdAndUser_IdNot(conversationType, conversationId, currentUserId)
            .map(ConversationReadStateEntity::getLastReadAt)
            .orElse(null);
    }

    public LocalDateTime getReadAt(String conversationType, String conversationId, String userId) {
        return getLastReadAt(conversationType, conversationId, userId);
    }

    @Transactional
    public LocalDateTime markRead(String conversationType, String conversationId, UserEntity user, LocalDateTime latestMessageAt) {
        LocalDateTime readAt = latestMessageAt == null ? LocalDateTime.now() : latestMessageAt;
        conversationReadStateRepository.upsertReadState(
            UUID.randomUUID().toString(),
            conversationType,
            conversationId,
            user.getId(),
            readAt,
            LocalDateTime.now()
        );
        return readAt;
    }
}
