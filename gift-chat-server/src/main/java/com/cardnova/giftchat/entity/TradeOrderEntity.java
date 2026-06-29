package com.cardnova.giftchat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

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

    @Column(name = "payout_amount", nullable = false, length = 32)
    private String payoutAmount;

    @Column(name = "status_code", nullable = false, length = 32)
    private String statusCode;

    @Column(name = "note", length = 255)
    private String note;

    @Column(name = "voucher_image_url", length = 255)
    private String voucherImageUrl;

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

    public String getPayoutAmount() {
        return payoutAmount;
    }

    public void setPayoutAmount(String payoutAmount) {
        this.payoutAmount = payoutAmount;
    }

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
