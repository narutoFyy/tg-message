package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.LotteryPrizeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LotteryPrizeRepository extends JpaRepository<LotteryPrizeEntity, String> {

    List<LotteryPrizeEntity> findByEnabledTrueOrderBySortOrderAsc();

    List<LotteryPrizeEntity> findAllByOrderBySortOrderAsc();
}
