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
@Table(name = "trade_order")
public class TradeOrderEntity {

    @Id
    private String id;

    @Column(name = "order_no", nullable = false, unique = true, length = 32)
    private String orderNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private UserEntity ownerUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counterparty_user_id", nullable = false)
    private UserEntity counterpartyUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "friendship_id")
    private FriendshipEntity friendship;

    @Column(name = "card_name", nullable = false, length = 128)
    private String cardName;

    @Column(name = "face_value", nullable = false, length = 32)
    private String faceValue;

    @Column(name = "face_currency_code", length = 3)
    private String faceCurrencyCode;

    @Column(name = "face_value_amount", precision = 18, scale = 6)
    private BigDecimal faceValueAmount;

    @Column(name = "quantity_value")
    private Integer quantityValue;

    @Column(name = "payout_amount", nullable = false, length = 32)
    private String payoutAmount;

    @Column(name = "base_amount_usd", precision = 18, scale = 6)
    private BigDecimal baseAmountUsd;

    @Column(name = "local_amount", precision = 18, scale = 2)
    private BigDecimal localAmount;

    @Column(name = "estimated_local_amount", precision = 18, scale = 2)
    private BigDecimal estimatedLocalAmount;

    @Column(name = "final_local_amount", precision = 18, scale = 2)
    private BigDecimal finalLocalAmount;

    @Column(name = "manual_vip_points", precision = 18, scale = 2)
    private BigDecimal manualVipPoints;

    @Column(name = "settlement_reason", length = 255)
    private String settlementReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "settled_by_user_id")
    private UserEntity settledByUser;

    @Column(name = "settled_at")
    private LocalDateTime settledAt;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "business_rate_snapshot", precision = 18, scale = 6)
    private BigDecimal businessRateSnapshot;

    @Column(name = "face_to_usd_rate_snapshot", precision = 18, scale = 6)
    private BigDecimal faceToUsdRateSnapshot;

    @Column(name = "client_request_id", length = 64)
    private String clientRequestId;

    @Column(name = "client_request_hash", length = 64)
    private String clientRequestHash;

    @Column(name = "status_code", nullable = false, length = 32)
    private String statusCode;

    @Column(name = "note", length = 255)
    private String note;

    @Column(name = "voucher_image_url", length = 255)
    private String voucherImageUrl;

    @Column(name = "cancel_reason", length = 64)
    private String cancelReason;

    @Column(name = "cancel_note", length = 255)
    private String cancelNote;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "canceled_by_user_id")
    private UserEntity canceledByUser;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public UserEntity getOwnerUser() {
        return ownerUser;
    }

    public void setOwnerUser(UserEntity ownerUser) {
        this.ownerUser = ownerUser;
    }

    public UserEntity getCounterpartyUser() {
        return counterpartyUser;
    }

    public void setCounterpartyUser(UserEntity counterpartyUser) {
        this.counterpartyUser = counterpartyUser;
    }

    public FriendshipEntity getFriendship() {
        return friendship;
    }

    public void setFriendship(FriendshipEntity friendship) {
        this.friendship = friendship;
    }

    public String getCardName() {
        return cardName;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    public String getFaceValue() {
        return faceValue;
    }

    public void setFaceValue(String faceValue) {
        this.faceValue = faceValue;
    }

    public String getFaceCurrencyCode() { return faceCurrencyCode; }
    public void setFaceCurrencyCode(String faceCurrencyCode) { this.faceCurrencyCode = faceCurrencyCode; }
    public BigDecimal getFaceValueAmount() { return faceValueAmount; }
    public void setFaceValueAmount(BigDecimal faceValueAmount) { this.faceValueAmount = faceValueAmount; }
    public Integer getQuantityValue() { return quantityValue; }
    public void setQuantityValue(Integer quantityValue) { this.quantityValue = quantityValue; }

    public String getPayoutAmount() {
        return payoutAmount;
    }

    public void setPayoutAmount(String payoutAmount) {
        this.payoutAmount = payoutAmount;
    }

    public BigDecimal getBaseAmountUsd() { return baseAmountUsd; }
    public void setBaseAmountUsd(BigDecimal baseAmountUsd) { this.baseAmountUsd = baseAmountUsd; }
    public BigDecimal getLocalAmount() { return localAmount; }
    public void setLocalAmount(BigDecimal localAmount) { this.localAmount = localAmount; }
    public BigDecimal getEstimatedLocalAmount() { return estimatedLocalAmount; }
    public void setEstimatedLocalAmount(BigDecimal estimatedLocalAmount) { this.estimatedLocalAmount = estimatedLocalAmount; }
    public BigDecimal getFinalLocalAmount() { return finalLocalAmount; }
    public void setFinalLocalAmount(BigDecimal finalLocalAmount) { this.finalLocalAmount = finalLocalAmount; }
    public BigDecimal getManualVipPoints() { return manualVipPoints; }
    public void setManualVipPoints(BigDecimal manualVipPoints) { this.manualVipPoints = manualVipPoints; }
    public String getSettlementReason() { return settlementReason; }
    public void setSettlementReason(String settlementReason) { this.settlementReason = settlementReason; }
    public UserEntity getSettledByUser() { return settledByUser; }
    public void setSettledByUser(UserEntity settledByUser) { this.settledByUser = settledByUser; }
    public LocalDateTime getSettledAt() { return settledAt; }
    public void setSettledAt(LocalDateTime settledAt) { this.settledAt = settledAt; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public BigDecimal getBusinessRateSnapshot() { return businessRateSnapshot; }
    public void setBusinessRateSnapshot(BigDecimal businessRateSnapshot) { this.businessRateSnapshot = businessRateSnapshot; }
    public BigDecimal getFaceToUsdRateSnapshot() { return faceToUsdRateSnapshot; }
    public void setFaceToUsdRateSnapshot(BigDecimal faceToUsdRateSnapshot) { this.faceToUsdRateSnapshot = faceToUsdRateSnapshot; }
    public String getClientRequestId() { return clientRequestId; }
    public void setClientRequestId(String clientRequestId) { this.clientRequestId = clientRequestId; }
    public String getClientRequestHash() { return clientRequestHash; }
    public void setClientRequestHash(String clientRequestHash) { this.clientRequestHash = clientRequestHash; }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getVoucherImageUrl() {
        return voucherImageUrl;
    }

    public void setVoucherImageUrl(String voucherImageUrl) {
        this.voucherImageUrl = voucherImageUrl;
    }

    public String getCancelReason() {
        return cancelReason;
    }

    public void setCancelReason(String cancelReason) {
        this.cancelReason = cancelReason;
    }

    public String getCancelNote() {
        return cancelNote;
    }

    public void setCancelNote(String cancelNote) {
        this.cancelNote = cancelNote;
    }

    public UserEntity getCanceledByUser() {
        return canceledByUser;
    }

    public void setCanceledByUser(UserEntity canceledByUser) {
        this.canceledByUser = canceledByUser;
    }

    public LocalDateTime getCanceledAt() {
        return canceledAt;
    }

    public void setCanceledAt(LocalDateTime canceledAt) {
        this.canceledAt = canceledAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
