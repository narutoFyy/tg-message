package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.BlacklistEntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BlacklistEntryRepository extends JpaRepository<BlacklistEntryEntity, String> {

    List<BlacklistEntryEntity> findByOwnerUser_Id(String ownerUserId);

    boolean existsByOwnerUser_IdAndBlockedUser_Id(String ownerUserId, String blockedUserId);

    boolean existsByBlockedUser_Id(String blockedUserId);

    Optional<BlacklistEntryEntity> findByIdAndOwnerUser_Id(String id, String ownerUserId);
}
