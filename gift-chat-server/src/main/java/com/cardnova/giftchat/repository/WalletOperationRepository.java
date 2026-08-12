package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.WalletOperationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;

public interface WalletOperationRepository extends JpaRepository<WalletOperationEntity, String> {

    @Query("select coalesce(sum(operation.amountDelta), 0) from WalletOperationEntity operation "
        + "where operation.user.id in :userIds "
        + "and operation.actionType in ('MANUAL_ADD', 'MANUAL_SUBTRACT')")
    BigDecimal sumAmountDeltaByUserIds(@Param("userIds") Collection<String> userIds);
}
