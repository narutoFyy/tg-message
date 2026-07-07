package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.VipPointLedgerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.Collection;

public interface VipPointLedgerRepository extends JpaRepository<VipPointLedgerEntity, String> {

    boolean existsBySourceKey(String sourceKey);

    @Query("select coalesce(sum(item.pointsDelta), 0) from VipPointLedgerEntity item where item.user.id = :userId")
    BigDecimal sumPointsByUserId(String userId);

    @Query("select coalesce(sum(item.pointsDelta), 0) from VipPointLedgerEntity item where item.user.id in :userIds")
    BigDecimal sumPointsByUserIds(Collection<String> userIds);
}
