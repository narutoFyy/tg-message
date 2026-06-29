package com.cardnova.giftchat.service;

import com.cardnova.giftchat.config.ChatWebSocketHandler.PresenceChangedEvent;
import com.cardnova.giftchat.entity.SupportConversationEntity;
import com.cardnova.giftchat.repository.SupportConversationRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

@Service
public class SupportPresenceNotifier {

    private final SupportConversationRepository supportConversationRepository;
    private final RealtimeChatService realtimeChatService;

    public SupportPresenceNotifier(
        SupportConversationRepository supportConversationRepository,
        RealtimeChatService realtimeChatService
    ) {
        this.supportConversationRepository = supportConversationRepository;
        this.realtimeChatService = realtimeChatService;
    }

    @EventListener
    public void handle(PresenceChangedEvent event) {
        supportConversationRepository.findByCustomerUser_IdOrderByUpdatedAtDesc(event.userId()).stream()
            .map(SupportConversationEntity::getId)
            .forEach(conversationId -> realtimeChatService.broadcastPresence(
                RealtimeChatService.supportChannel(conversationId),
                "support",
                conversationId,
                event.userId(),
                event.online()
            ));
    }
}
