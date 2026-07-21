package com.cardnova.giftchat.repository;

import com.cardnova.giftchat.entity.TradeOrderSettlementAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeOrderSettlementAuditRepository extends JpaRepository<TradeOrderSettlementAuditEntity, String> {

    boolean existsByTradeOrder_IdAndActionCode(String tradeOrderId, String actionCode);
}
