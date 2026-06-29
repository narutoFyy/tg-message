package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.GiftCardRateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GiftCardRateRepository extends JpaRepository<GiftCardRateEntity, String> {

    List<GiftCardRateEntity> findAllByOrderByUpdatedAtDesc();
}
