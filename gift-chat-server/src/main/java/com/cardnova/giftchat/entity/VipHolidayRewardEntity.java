package com.cardnova.giftchat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vip_holiday_reward")
public class VipHolidayRewardEntity {
    @Id private String id;
    @Column(name = "country_code", nullable = false, length = 2) private String countryCode;
    @Column(name = "holiday_code", nullable = false, length = 64) private String holidayCode;
    @Column(name = "holiday_name", nullable = false, length = 128) private String holidayName;
    @Column(name = "holiday_date", nullable = false) private LocalDate holidayDate;
    @Column(name = "reward_amount", nullable = false, precision = 18, scale = 2) private BigDecimal rewardAmount;
    @Column(name = "currency_code", nullable = false, length = 3) private String currencyCode;
    @Column(nullable = false) private boolean enabled;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "updated_by") private UserEntity updatedBy;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    public String getId() { return id; } public void setId(String id) { this.id = id; }
    public String getCountryCode() { return countryCode; } public void setCountryCode(String value) { this.countryCode = value; }
    public String getHolidayCode() { return holidayCode; } public void setHolidayCode(String value) { this.holidayCode = value; }
    public String getHolidayName() { return holidayName; } public void setHolidayName(String value) { this.holidayName = value; }
    public LocalDate getHolidayDate() { return holidayDate; } public void setHolidayDate(LocalDate value) { this.holidayDate = value; }
    public BigDecimal getRewardAmount() { return rewardAmount; } public void setRewardAmount(BigDecimal value) { this.rewardAmount = value; }
    public String getCurrencyCode() { return currencyCode; } public void setCurrencyCode(String value) { this.currencyCode = value; }
    public boolean isEnabled() { return enabled; } public void setEnabled(boolean value) { this.enabled = value; }
    public UserEntity getUpdatedBy() { return updatedBy; } public void setUpdatedBy(UserEntity value) { this.updatedBy = value; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime value) { this.createdAt = value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime value) { this.updatedAt = value; }
}
