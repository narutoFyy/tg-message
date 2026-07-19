package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.GiftCardRateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GiftCardRateRepository extends JpaRepository<GiftCardRateEntity, String> {

    List<GiftCardRateEntity> findAllByOrderByUpdatedAtDesc();

    Optional<GiftCardRateEntity> findFirstByCardNameIgnoreCaseAndRegionCodeIgnoreCaseAndStatusCodeIgnoreCase(
        String cardName,
        String regionCode,
        String statusCode
    );

    Optional<GiftCardRateEntity> findByRegionCodeIgnoreCaseAndIdentityKey(String regionCode, String identityKey);
}
