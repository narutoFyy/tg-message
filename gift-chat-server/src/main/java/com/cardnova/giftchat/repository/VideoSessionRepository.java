package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.VideoSessionEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VideoSessionRepository extends JpaRepository<VideoSessionEntity, String> {

    Optional<VideoSessionEntity> findByRoomId(String roomId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select session from VideoSessionEntity session where session.id = :sessionId")
    Optional<VideoSessionEntity> findByIdForUpdate(@Param("sessionId") String sessionId);

    List<VideoSessionEntity> findByInitiatorUser_IdOrReceiverUser_IdOrderByCreatedAtDesc(String initiatorUserId, String receiverUserId);

    List<VideoSessionEntity> findByChannelTypeAndChannelIdOrderByCreatedAtDesc(String channelType, String channelId);

    List<VideoSessionEntity> findAllByOrderByCreatedAtDesc();
}
