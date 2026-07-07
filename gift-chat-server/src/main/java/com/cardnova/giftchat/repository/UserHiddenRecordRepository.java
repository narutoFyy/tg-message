package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.UserHiddenRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserHiddenRecordRepository extends JpaRepository<UserHiddenRecordEntity, String> {

    boolean existsByUser_IdAndTargetTypeAndTargetIdAndHiddenScopeAndRestoredAtIsNull(
        String userId,
        String targetType,
        String targetId,
        String hiddenScope
    );

    Optional<UserHiddenRecordEntity> findByUser_IdAndTargetTypeAndTargetIdAndHiddenScope(
        String userId,
        String targetType,
        String targetId,
        String hiddenScope
    );

    List<UserHiddenRecordEntity> findByUser_IdAndTargetTypeAndHiddenScopeAndRestoredAtIsNull(
        String userId,
        String targetType,
        String hiddenScope
    );

    List<UserHiddenRecordEntity> findByUser_IdAndTargetTypeAndTargetIdInAndRestoredAtIsNull(
        String userId,
        String targetType,
        Collection<String> targetIds
    );

    List<UserHiddenRecordEntity> findByUser_IdAndRestoredAtIsNullOrderByCreatedAtDesc(String userId);
}
