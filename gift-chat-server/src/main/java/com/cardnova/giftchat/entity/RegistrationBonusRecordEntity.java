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
@Table(name = "registration_bonus_record")
public class RegistrationBonusRecordEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "phone_snapshot", length = 32)
    private String phoneSnapshot;

    @Column(name = "country_code", length = 8)
    private String countryCode;

    @Column(name = "country_name", length = 64)
    private String countryName;

    @Column(name = "currency_code", length = 16)
    private String currencyCode;

    @Column(name = "bonus_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal bonusAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id")
    private RegistrationBonusConfigEntity config;

    @Column(name = "status_code", nullable = false, length = 32)
    private String statusCode;

    @Column(name = "reason_note", length = 255)
    private String reasonNote;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unlocked_by_order_id")
    private TradeOrderEntity unlockedByOrder;

    @Column(name = "unlocked_at")
    private LocalDateTime unlockedAt;

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

    public String getPhoneSnapshot() {
        return phoneSnapshot;
    }

    public void setPhoneSnapshot(String phoneSnapshot) {
        this.phoneSnapshot = phoneSnapshot;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getCountryName() {
        return countryName;
    }

    public void setCountryName(String countryName) {
        this.countryName = countryName;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public BigDecimal getBonusAmount() {
        return bonusAmount;
    }

    public void setBonusAmount(BigDecimal bonusAmount) {
        this.bonusAmount = bonusAmount;
    }

    public RegistrationBonusConfigEntity getConfig() {
        return config;
    }

    public void setConfig(RegistrationBonusConfigEntity config) {
        this.config = config;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getReasonNote() {
        return reasonNote;
    }

    public void setReasonNote(String reasonNote) {
        this.reasonNote = reasonNote;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public TradeOrderEntity getUnlockedByOrder() { return unlockedByOrder; }
    public void setUnlockedByOrder(TradeOrderEntity unlockedByOrder) { this.unlockedByOrder = unlockedByOrder; }
    public LocalDateTime getUnlockedAt() { return unlockedAt; }
    public void setUnlockedAt(LocalDateTime unlockedAt) { this.unlockedAt = unlockedAt; }
}
