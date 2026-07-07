package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.LotteryDrawRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LotteryDrawRecordRepository extends JpaRepository<LotteryDrawRecordEntity, String> {

    boolean existsByUser_IdAndPeriodTypeAndPeriodKey(String userId, String periodType, String periodKey);

    long countByUser_IdAndPeriodTypeAndPeriodKey(String userId, String periodType, String periodKey);

    Optional<LotteryDrawRecordEntity> findFirstByUser_IdAndPeriodTypeAndPeriodKeyOrderByDrawnAtDesc(String userId, String periodType, String periodKey);

    Optional<LotteryDrawRecordEntity> findFirstByUser_IdOrderByDrawnAtDesc(String userId);

    List<LotteryDrawRecordEntity> findTop50ByOrderByDrawnAtDesc();

    List<LotteryDrawRecordEntity> findAllByOrderByDrawnAtDesc();
}
