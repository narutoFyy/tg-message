package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.PushDeviceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PushDeviceRepository extends JpaRepository<PushDeviceEntity, String> {

    Optional<PushDeviceEntity> findByProviderAndDeviceToken(String provider, String deviceToken);

    List<PushDeviceEntity> findByUser_IdAndEnabledTrue(String userId);
}
