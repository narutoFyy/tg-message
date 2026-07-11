package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.LotteryFulfillmentOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LotteryFulfillmentOrderRepository extends JpaRepository<LotteryFulfillmentOrderEntity, String> {

    boolean existsByLotteryDrawRecord_Id(String lotteryDrawRecordId);

    Optional<LotteryFulfillmentOrderEntity> findByLotteryDrawRecord_Id(String lotteryDrawRecordId);

    List<LotteryFulfillmentOrderEntity> findByOwnerUser_IdOrderByUpdatedAtDesc(String ownerUserId);

    List<LotteryFulfillmentOrderEntity> findByAssignedAgent_IdOrderByUpdatedAtDesc(String assignedAgentId);

    List<LotteryFulfillmentOrderEntity> findAllByOrderByUpdatedAtDesc();

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
