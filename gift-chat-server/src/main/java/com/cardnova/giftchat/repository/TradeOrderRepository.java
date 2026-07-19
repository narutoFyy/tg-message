package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.TradeOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TradeOrderRepository extends JpaRepository<TradeOrderEntity, String> {

    List<TradeOrderEntity> findByOwnerUser_IdOrCounterpartyUser_IdOrderByUpdatedAtDesc(String ownerUserId, String counterpartyUserId);

    List<TradeOrderEntity> findByOwnerUser_IdOrderByUpdatedAtDesc(String ownerUserId);

    List<TradeOrderEntity> findByOwnerUser_IdIn(List<String> ownerUserIds);

    List<TradeOrderEntity> findAllByOrderByUpdatedAtDesc();

    List<TradeOrderEntity> findTop12ByStatusCodeOrderByUpdatedAtDesc(String statusCode);

    Optional<TradeOrderEntity> findByOwnerUser_IdAndClientRequestId(String ownerUserId, String clientRequestId);
}
