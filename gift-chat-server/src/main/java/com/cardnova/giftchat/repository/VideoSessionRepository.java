package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.VideoSessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VideoSessionRepository extends JpaRepository<VideoSessionEntity, String> {

    Optional<VideoSessionEntity> findByRoomId(String roomId);

    List<VideoSessionEntity> findByInitiatorUser_IdOrReceiverUser_IdOrderByCreatedAtDesc(String initiatorUserId, String receiverUserId);

    List<VideoSessionEntity> findByChannelTypeAndChannelIdOrderByCreatedAtDesc(String channelType, String channelId);

    List<VideoSessionEntity> findAllByOrderByCreatedAtDesc();
}
