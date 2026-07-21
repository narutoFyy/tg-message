package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.TradeOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

import java.util.List;
import java.util.Optional;

public interface TradeOrderRepository extends JpaRepository<TradeOrderEntity, String> {

    List<TradeOrderEntity> findByOwnerUser_IdOrCounterpartyUser_IdOrderByUpdatedAtDesc(String ownerUserId, String counterpartyUserId);

    List<TradeOrderEntity> findByOwnerUser_IdOrderByUpdatedAtDesc(String ownerUserId);

    List<TradeOrderEntity> findByOwnerUser_IdIn(List<String> ownerUserIds);

    List<TradeOrderEntity> findAllByOrderByUpdatedAtDesc();

    List<TradeOrderEntity> findTop12ByStatusCodeOrderByUpdatedAtDesc(String statusCode);

    Optional<TradeOrderEntity> findByOwnerUser_IdAndClientRequestId(String ownerUserId, String clientRequestId);

    boolean existsByOwnerUser_IdAndStatusCodeIgnoreCase(String ownerUserId, String statusCode);

    @Query("""
        select coalesce(sum(item.baseAmountUsd), 0)
        from TradeOrderEntity item
        where item.ownerUser.id = :userId and upper(item.statusCode) = 'COMPLETED'
        """)
    BigDecimal sumCompletedBaseAmountUsdByOwnerUserId(String userId);
}
