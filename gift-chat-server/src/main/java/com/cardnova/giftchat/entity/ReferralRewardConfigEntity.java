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
@Table(name = "referral_reward_config")
public class ReferralRewardConfigEntity {

    @Id
    private String id;

    @Column(name = "registration_cashback_enabled", nullable = false)
    private boolean registrationCashbackEnabled;

    @Column(name = "registration_cashback_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal registrationCashbackAmount;

    @Column(name = "trade_rebate_enabled", nullable = false)
    private boolean tradeRebateEnabled;

    @Column(name = "trade_rebate_percent", nullable = false, precision = 8, scale = 4)
    private BigDecimal tradeRebatePercent;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private UserEntity updatedBy;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public boolean isRegistrationCashbackEnabled() {
        return registrationCashbackEnabled;
    }

    public void setRegistrationCashbackEnabled(boolean registrationCashbackEnabled) {
        this.registrationCashbackEnabled = registrationCashbackEnabled;
    }

    public BigDecimal getRegistrationCashbackAmount() {
        return registrationCashbackAmount;
    }

    public void setRegistrationCashbackAmount(BigDecimal registrationCashbackAmount) {
        this.registrationCashbackAmount = registrationCashbackAmount;
    }

    public boolean isTradeRebateEnabled() {
        return tradeRebateEnabled;
    }

    public void setTradeRebateEnabled(boolean tradeRebateEnabled) {
        this.tradeRebateEnabled = tradeRebateEnabled;
    }

    public BigDecimal getTradeRebatePercent() {
        return tradeRebatePercent;
    }

    public void setTradeRebatePercent(BigDecimal tradeRebatePercent) {
        this.tradeRebatePercent = tradeRebatePercent;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public UserEntity getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UserEntity updatedBy) {
        this.updatedBy = updatedBy;
    }
}
