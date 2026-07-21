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
@Table(name = "gift_card_rate_quote")
public class GiftCardRateQuoteEntity {
    @Id private String id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "rate_id", nullable = false) private GiftCardRateEntity rate;
    @Column(name = "face_currency_code", nullable = false, length = 3) private String faceCurrencyCode;
    @Column(name = "local_payout_per_unit", nullable = false, precision = 18, scale = 6) private BigDecimal localPayoutPerUnit;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public GiftCardRateEntity getRate() { return rate; }
    public void setRate(GiftCardRateEntity rate) { this.rate = rate; }
    public String getFaceCurrencyCode() { return faceCurrencyCode; }
    public void setFaceCurrencyCode(String faceCurrencyCode) { this.faceCurrencyCode = faceCurrencyCode; }
    public BigDecimal getLocalPayoutPerUnit() { return localPayoutPerUnit; }
    public void setLocalPayoutPerUnit(BigDecimal localPayoutPerUnit) { this.localPayoutPerUnit = localPayoutPerUnit; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
