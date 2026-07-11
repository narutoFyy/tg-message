package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.WithdrawalRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequestEntity, String> {

    List<WithdrawalRequestEntity> findByOwnerUser_IdOrderByUpdatedAtDesc(String ownerUserId);

    List<WithdrawalRequestEntity> findByOwnerUser_IdIn(List<String> ownerUserIds);

    List<WithdrawalRequestEntity> findByAssignedAgent_IdOrderByUpdatedAtDesc(String assignedAgentId);

    List<WithdrawalRequestEntity> findAllByOrderByUpdatedAtDesc();

    boolean existsByLotteryDrawRecord_Id(String lotteryDrawRecordId);

    Optional<WithdrawalRequestEntity> findByLotteryDrawRecord_Id(String lotteryDrawRecordId);

    boolean existsByOwnerUser_IdAndStatusCode(String ownerUserId, String statusCode);

    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
