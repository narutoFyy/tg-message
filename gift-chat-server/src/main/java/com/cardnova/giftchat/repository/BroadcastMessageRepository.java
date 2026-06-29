package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.BroadcastMessageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BroadcastMessageRepository extends JpaRepository<BroadcastMessageEntity, String> {

    List<BroadcastMessageEntity> findAllByOrderByCreatedAtDesc();

    List<BroadcastMessageEntity> findBySenderUser_IdOrderByCreatedAtDesc(String senderUserId);
}
