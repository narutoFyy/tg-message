package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.UserBankAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserBankAccountRepository extends JpaRepository<UserBankAccountEntity, String> {

    Optional<UserBankAccountEntity> findByOwnerUser_Id(String ownerUserId);

    boolean existsByOwnerUser_Id(String ownerUserId);

    boolean existsByAccountFingerprint(String accountFingerprint);

    List<UserBankAccountEntity> findAllByOrderByCreatedAtDesc();
}
