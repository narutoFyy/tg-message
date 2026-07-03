package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.ReferralRewardEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReferralRewardRepository extends JpaRepository<ReferralRewardEntity, String> {

    boolean existsByRewardTypeAndSourceKey(String rewardType, String sourceKey);

    long countByRewardTypeAndSourceKey(String rewardType, String sourceKey);

    List<ReferralRewardEntity> findByReferrerUser_IdIn(List<String> referrerUserIds);

    List<ReferralRewardEntity> findByReferrerUser_IdOrderByCreatedAtDesc(String referrerUserId);

    List<ReferralRewardEntity> findAllByOrderByCreatedAtDesc();
}
