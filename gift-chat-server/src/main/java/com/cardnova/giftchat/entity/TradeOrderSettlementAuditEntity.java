package com.cardnova.giftchat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trade_order_settlement_audit")
public class TradeOrderSettlementAuditEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_order_id", nullable = false)
    private TradeOrderEntity tradeOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_user_id", nullable = false)
    private UserEntity operatorUser;

    @Column(name = "action_code", nullable = false, length = 32)
    private String actionCode;

    @Column(name = "estimated_local_amount", precision = 18, scale = 2)
    private BigDecimal estimatedLocalAmount;

    @Column(name = "final_local_amount", precision = 18, scale = 2)
    private BigDecimal finalLocalAmount;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "vip_points", nullable = false, precision = 18, scale = 2)
    private BigDecimal vipPoints;

    @Column(name = "reason_note", length = 255)
    private String reasonNote;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public TradeOrderEntity getTradeOrder() { return tradeOrder; }
    public void setTradeOrder(TradeOrderEntity tradeOrder) { this.tradeOrder = tradeOrder; }
    public UserEntity getOperatorUser() { return operatorUser; }
    public void setOperatorUser(UserEntity operatorUser) { this.operatorUser = operatorUser; }
    public String getActionCode() { return actionCode; }
    public void setActionCode(String actionCode) { this.actionCode = actionCode; }
    public BigDecimal getEstimatedLocalAmount() { return estimatedLocalAmount; }
    public void setEstimatedLocalAmount(BigDecimal estimatedLocalAmount) { this.estimatedLocalAmount = estimatedLocalAmount; }
    public BigDecimal getFinalLocalAmount() { return finalLocalAmount; }
    public void setFinalLocalAmount(BigDecimal finalLocalAmount) { this.finalLocalAmount = finalLocalAmount; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public BigDecimal getVipPoints() { return vipPoints; }
    public void setVipPoints(BigDecimal vipPoints) { this.vipPoints = vipPoints; }
    public String getReasonNote() { return reasonNote; }
    public void setReasonNote(String reasonNote) { this.reasonNote = reasonNote; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
