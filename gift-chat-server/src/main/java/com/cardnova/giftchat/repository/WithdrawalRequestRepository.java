package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.WithdrawalRequestEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequestEntity, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select withdrawal from WithdrawalRequestEntity withdrawal where withdrawal.id = :withdrawalId")
    Optional<WithdrawalRequestEntity> findByIdForUpdate(@Param("withdrawalId") String withdrawalId);

    List<WithdrawalRequestEntity> findByOwnerUser_IdOrderByUpdatedAtDesc(String ownerUserId);

    List<WithdrawalRequestEntity> findByOwnerUser_IdIn(List<String> ownerUserIds);

    List<WithdrawalRequestEntity> findByAssignedAgent_IdOrderByUpdatedAtDesc(String assignedAgentId);

    List<WithdrawalRequestEntity> findAllByOrderByUpdatedAtDesc();

    boolean existsByLotteryDrawRecord_Id(String lotteryDrawRecordId);

    Optional<WithdrawalRequestEntity> findByLotteryDrawRecord_Id(String lotteryDrawRecordId);

    boolean existsByOwnerUser_IdAndStatusCode(String ownerUserId, String statusCode);

}
