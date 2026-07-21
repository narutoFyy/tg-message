package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.GiftCardArtworkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GiftCardArtworkRepository extends JpaRepository<GiftCardArtworkEntity, String> {
}
