package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.DirectMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DirectMessageRepository extends JpaRepository<DirectMessageEntity, String> {

    List<DirectMessageEntity> findByFriendship_IdOrderByCreatedAtAsc(String friendshipId);
}
