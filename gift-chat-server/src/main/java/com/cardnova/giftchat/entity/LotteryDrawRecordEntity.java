package com.cardnova.giftchat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Entity
@Table(name = "lottery_draw_record")
public class LotteryDrawRecordEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "vip_level", nullable = false, length = 16)
    private String vipLevel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prize_id", nullable = false)
    private LotteryPrizeEntity prize;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lottery_chance_id")
    private LotteryChanceEntity lotteryChance;

    @Column(name = "period_type", nullable = false, length = 16)
    private String periodType;

    @Column(name = "period_key", nullable = false, length = 32)
    private String periodKey;

    @Column(name = "drawn_at", nullable = false)
    private LocalDateTime drawnAt;

    @Column(name = "fulfillment_status", nullable = false, length = 32)
    private String fulfillmentStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private UserEntity processedBy;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "base_amount_usd", precision = 18, scale = 6)
    private BigDecimal baseAmountUsd;

    @Column(name = "local_amount", precision = 18, scale = 2)
    private BigDecimal localAmount;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "exchange_rate_snapshot", precision = 18, scale = 6)
    private BigDecimal exchangeRateSnapshot;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UserEntity getUser() {
        return user;
    }

    public void setUser(UserEntity user) {
        this.user = user;
    }

    public String getVipLevel() {
        return vipLevel;
    }

    public void setVipLevel(String vipLevel) {
        this.vipLevel = vipLevel;
    }

    public LotteryPrizeEntity getPrize() {
        return prize;
    }

    public void setPrize(LotteryPrizeEntity prize) {
        this.prize = prize;
    }

    public LotteryChanceEntity getLotteryChance() { return lotteryChance; }
    public void setLotteryChance(LotteryChanceEntity lotteryChance) { this.lotteryChance = lotteryChance; }

    public String getPeriodType() {
        return periodType;
    }

    public void setPeriodType(String periodType) {
        this.periodType = periodType;
    }

    public String getPeriodKey() {
        return periodKey;
    }

    public void setPeriodKey(String periodKey) {
        this.periodKey = periodKey;
    }

    public LocalDateTime getDrawnAt() {
        return drawnAt;
    }

    public void setDrawnAt(LocalDateTime drawnAt) {
        this.drawnAt = drawnAt;
    }

    public String getFulfillmentStatus() {
        return fulfillmentStatus;
    }

    public void setFulfillmentStatus(String fulfillmentStatus) {
        this.fulfillmentStatus = fulfillmentStatus;
    }

    public UserEntity getProcessedBy() {
        return processedBy;
    }

    public void setProcessedBy(UserEntity processedBy) {
        this.processedBy = processedBy;
    }

    public LocalDateTime getProcessedAt() {
        return processedAt;
    }

    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }

    public BigDecimal getBaseAmountUsd() { return baseAmountUsd; }
    public void setBaseAmountUsd(BigDecimal baseAmountUsd) { this.baseAmountUsd = baseAmountUsd; }
    public BigDecimal getLocalAmount() { return localAmount; }
    public void setLocalAmount(BigDecimal localAmount) { this.localAmount = localAmount; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public BigDecimal getExchangeRateSnapshot() { return exchangeRateSnapshot; }
    public void setExchangeRateSnapshot(BigDecimal exchangeRateSnapshot) { this.exchangeRateSnapshot = exchangeRateSnapshot; }
}
