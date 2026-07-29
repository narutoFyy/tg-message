package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.RegistrationBonusRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RegistrationBonusRecordRepository extends JpaRepository<RegistrationBonusRecordEntity, String> {

    boolean existsByUser_Id(String userId);

    long countByUser_Id(String userId);

    Optional<RegistrationBonusRecordEntity> findByUser_Id(String userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select record from RegistrationBonusRecordEntity record where record.user.id = :userId")
    Optional<RegistrationBonusRecordEntity> findByUser_IdForUpdate(@Param("userId") String userId);

    List<RegistrationBonusRecordEntity> findByUser_IdInAndStatusCode(Collection<String> userIds, String statusCode);

    List<RegistrationBonusRecordEntity> findAllByOrderByCreatedAtDesc();
}
