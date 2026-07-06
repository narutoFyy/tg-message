package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.RegistrationBonusRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RegistrationBonusRecordRepository extends JpaRepository<RegistrationBonusRecordEntity, String> {

    boolean existsByUser_Id(String userId);

    long countByUser_Id(String userId);

    Optional<RegistrationBonusRecordEntity> findByUser_Id(String userId);

    List<RegistrationBonusRecordEntity> findByUser_IdInAndStatusCode(Collection<String> userIds, String statusCode);

    List<RegistrationBonusRecordEntity> findAllByOrderByCreatedAtDesc();
}
