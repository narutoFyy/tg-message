package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<NotificationEntity, String> {

    List<NotificationEntity> findByRecipientUser_IdOrderByCreatedAtDesc(String recipientUserId);

    long countByRecipientUser_IdAndReadFlagFalse(String recipientUserId);
}
