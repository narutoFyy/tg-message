package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.UploadAssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UploadAssetRepository extends JpaRepository<UploadAssetEntity, String> {
}
