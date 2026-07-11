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
@Table(name = "lottery_fulfillment_order")
public class LotteryFulfillmentOrderEntity {

    @Id
    private String id;

    @Column(name = "order_no", nullable = false, unique = true, length = 32)
    private String orderNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private UserEntity ownerUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_agent_id")
    private UserEntity assignedAgent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lottery_draw_record_id", nullable = false, unique = true)
    private LotteryDrawRecordEntity lotteryDrawRecord;

    @Column(name = "recipient_name", nullable = false, length = 128)
    private String recipientName;

    @Column(nullable = false, length = 64)
    private String phone;

    @Column(nullable = false, length = 64)
    private String country;

    @Column(name = "address_line", nullable = false, length = 512)
    private String addressLine;

    @Column(name = "status_code", nullable = false, length = 32)
    private String statusCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public UserEntity getOwnerUser() { return ownerUser; }
    public void setOwnerUser(UserEntity ownerUser) { this.ownerUser = ownerUser; }
    public UserEntity getAssignedAgent() { return assignedAgent; }
    public void setAssignedAgent(UserEntity assignedAgent) { this.assignedAgent = assignedAgent; }
    public LotteryDrawRecordEntity getLotteryDrawRecord() { return lotteryDrawRecord; }
    public void setLotteryDrawRecord(LotteryDrawRecordEntity lotteryDrawRecord) { this.lotteryDrawRecord = lotteryDrawRecord; }
    public String getRecipientName() { return recipientName; }
    public void setRecipientName(String recipientName) { this.recipientName = recipientName; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }
    public String getAddressLine() { return addressLine; }
    public void setAddressLine(String addressLine) { this.addressLine = addressLine; }
    public String getStatusCode() { return statusCode; }
    public void setStatusCode(String statusCode) { this.statusCode = statusCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
