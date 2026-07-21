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
@Table(name = "vip_benefit_claim")
public class VipBenefitClaimEntity {
    @Id private String id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private UserEntity user;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "holiday_reward_id") private VipHolidayRewardEntity holidayReward;
    @Column(name = "benefit_type", nullable = false, length = 32) private String benefitType;
    @Column(name = "source_key", nullable = false, unique = true, length = 160) private String sourceKey;
    @Column(name = "period_key", nullable = false, length = 32) private String periodKey;
    @Column(name = "vip_level", nullable = false, length = 16) private String vipLevel;
    @Column(name = "status_code", nullable = false, length = 16) private String statusCode;
    @Column(name = "base_amount_usd", nullable = false, precision = 18, scale = 6) private BigDecimal baseAmountUsd;
    @Column(name = "local_amount", nullable = false, precision = 18, scale = 2) private BigDecimal localAmount;
    @Column(name = "currency_code", nullable = false, length = 3) private String currencyCode;
    @Column(name = "exchange_rate_snapshot", nullable = false, precision = 18, scale = 6) private BigDecimal exchangeRateSnapshot;
    @Column(name = "requested_at", nullable = false) private LocalDateTime requestedAt;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reviewed_by") private UserEntity reviewedBy;
    @Column(name = "reviewed_at") private LocalDateTime reviewedAt;
    @Column(name = "review_note", length = 255) private String reviewNote;
    public String getId() { return id; } public void setId(String id) { this.id = id; }
    public UserEntity getUser() { return user; } public void setUser(UserEntity value) { this.user = value; }
    public VipHolidayRewardEntity getHolidayReward() { return holidayReward; } public void setHolidayReward(VipHolidayRewardEntity value) { this.holidayReward = value; }
    public String getBenefitType() { return benefitType; } public void setBenefitType(String value) { this.benefitType = value; }
    public String getSourceKey() { return sourceKey; } public void setSourceKey(String value) { this.sourceKey = value; }
    public String getPeriodKey() { return periodKey; } public void setPeriodKey(String value) { this.periodKey = value; }
    public String getVipLevel() { return vipLevel; } public void setVipLevel(String value) { this.vipLevel = value; }
    public String getStatusCode() { return statusCode; } public void setStatusCode(String value) { this.statusCode = value; }
    public BigDecimal getBaseAmountUsd() { return baseAmountUsd; } public void setBaseAmountUsd(BigDecimal value) { this.baseAmountUsd = value; }
    public BigDecimal getLocalAmount() { return localAmount; } public void setLocalAmount(BigDecimal value) { this.localAmount = value; }
    public String getCurrencyCode() { return currencyCode; } public void setCurrencyCode(String value) { this.currencyCode = value; }
    public BigDecimal getExchangeRateSnapshot() { return exchangeRateSnapshot; } public void setExchangeRateSnapshot(BigDecimal value) { this.exchangeRateSnapshot = value; }
    public LocalDateTime getRequestedAt() { return requestedAt; } public void setRequestedAt(LocalDateTime value) { this.requestedAt = value; }
    public UserEntity getReviewedBy() { return reviewedBy; } public void setReviewedBy(UserEntity value) { this.reviewedBy = value; }
    public LocalDateTime getReviewedAt() { return reviewedAt; } public void setReviewedAt(LocalDateTime value) { this.reviewedAt = value; }
    public String getReviewNote() { return reviewNote; } public void setReviewNote(String value) { this.reviewNote = value; }
}
