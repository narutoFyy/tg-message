package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.WebPushSubscriptionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebPushSubscriptionRepository extends JpaRepository<WebPushSubscriptionEntity, String> {
    void deleteByUser_Id(String userId);
    List<WebPushSubscriptionEntity> findByUser_IdAndEnabledTrue(String userId);
}
