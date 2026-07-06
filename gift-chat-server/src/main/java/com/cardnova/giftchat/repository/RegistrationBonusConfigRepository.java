package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.RegistrationBonusConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegistrationBonusConfigRepository extends JpaRepository<RegistrationBonusConfigEntity, String> {

    Optional<RegistrationBonusConfigEntity> findByCountryCode(String countryCode);

    List<RegistrationBonusConfigEntity> findAllByOrderByCountryCodeAsc();
}
