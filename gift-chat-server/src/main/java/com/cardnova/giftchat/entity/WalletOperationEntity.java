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
@Table(name = "wallet_operation")
public class WalletOperationEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "operator_user_id", nullable = false)
    private UserEntity operatorUser;

    @Column(name = "action_type", nullable = false, length = 32)
    private String actionType;

    @Column(name = "amount_delta", nullable = false, precision = 18, scale = 2)
    private BigDecimal amountDelta;

    @Column(name = "currency_code", nullable = false, length = 16)
    private String currencyCode;

    @Column(name = "reference_id", length = 64)
    private String referenceId;

    @Column(nullable = false, length = 255)
    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }
    public UserEntity getOperatorUser() { return operatorUser; }
    public void setOperatorUser(UserEntity operatorUser) { this.operatorUser = operatorUser; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public BigDecimal getAmountDelta() { return amountDelta; }
    public void setAmountDelta(BigDecimal amountDelta) { this.amountDelta = amountDelta; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public String getReferenceId() { return referenceId; }
    public void setReferenceId(String referenceId) { this.referenceId = referenceId; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
