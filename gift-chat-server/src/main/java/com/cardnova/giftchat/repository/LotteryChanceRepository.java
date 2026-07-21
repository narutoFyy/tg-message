package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.LotteryChanceEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface LotteryChanceRepository extends JpaRepository<LotteryChanceEntity, String> {

    boolean existsBySourceKey(String sourceKey);

    long countByUser_IdAndConsumedAtIsNull(String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<LotteryChanceEntity> findFirstByUser_IdAndConsumedAtIsNullOrderByGrantedAtAsc(String userId);
}
