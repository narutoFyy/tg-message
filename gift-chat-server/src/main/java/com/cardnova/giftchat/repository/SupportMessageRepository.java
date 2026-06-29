package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.SupportMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupportMessageRepository extends JpaRepository<SupportMessageEntity, String> {

    List<SupportMessageEntity> findByConversation_IdOrderByCreatedAtAsc(String conversationId);
}
