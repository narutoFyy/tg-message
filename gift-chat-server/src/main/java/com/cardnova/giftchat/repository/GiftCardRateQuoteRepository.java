package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.GiftCardRateQuoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GiftCardRateQuoteRepository extends JpaRepository<GiftCardRateQuoteEntity, String> {
    List<GiftCardRateQuoteEntity> findByRate_IdOrderByFaceCurrencyCodeAsc(String rateId);
    Optional<GiftCardRateQuoteEntity> findByRate_IdAndFaceCurrencyCodeIgnoreCase(String rateId, String faceCurrencyCode);
    void deleteByRate_Id(String rateId);
}
