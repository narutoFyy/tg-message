package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.TradeOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TradeOrderRepository extends JpaRepository<TradeOrderEntity, String> {

    List<TradeOrderEntity> findByOwnerUser_IdOrCounterpartyUser_IdOrderByUpdatedAtDesc(String ownerUserId, String counterpartyUserId);

    List<TradeOrderEntity> findByOwnerUser_IdOrderByUpdatedAtDesc(String ownerUserId);

    List<TradeOrderEntity> findByOwnerUser_IdIn(List<String> ownerUserIds);

    List<TradeOrderEntity> findAllByOrderByUpdatedAtDesc();

    long countByCreatedAtBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
}
