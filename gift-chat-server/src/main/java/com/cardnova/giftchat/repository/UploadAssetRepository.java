package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.UploadAssetEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UploadAssetRepository extends JpaRepository<UploadAssetEntity, String> {
    Optional<UploadAssetEntity> findByPublicUrl(String publicUrl);
}
