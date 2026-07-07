package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.LotteryEligibilityResetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LotteryEligibilityResetRepository extends JpaRepository<LotteryEligibilityResetEntity, String> {

    Optional<LotteryEligibilityResetEntity> findFirstByUser_IdOrderByCreatedAtDesc(String userId);
}
