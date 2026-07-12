package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.CurrencyExchangeRateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CurrencyExchangeRateRepository extends JpaRepository<CurrencyExchangeRateEntity, String> {
    Optional<CurrencyExchangeRateEntity> findByCountryCode(String countryCode);
    List<CurrencyExchangeRateEntity> findAllByOrderByCountryCodeAsc();
}
