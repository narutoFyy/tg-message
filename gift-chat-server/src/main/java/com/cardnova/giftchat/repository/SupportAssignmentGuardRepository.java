package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.SupportAssignmentGuardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.Optional;

public interface SupportAssignmentGuardRepository extends JpaRepository<SupportAssignmentGuardEntity, Integer> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select guard from SupportAssignmentGuardEntity guard where guard.id = :id")
    Optional<SupportAssignmentGuardEntity> findByIdForUpdate(@Param("id") Integer id);
}
