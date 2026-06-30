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
@Table(name = "support_message")
public class SupportMessageEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private SupportConversationEntity conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_user_id")
    private UserEntity senderUser;

    @Column(name = "sender_role", nullable = false, length = 32)
    private String senderRole;

    @Column(name = "message_type", nullable = false, length = 32)
    private String messageType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "client_message_id", length = 64)
    private String clientMessageId;

    @Column(name = "server_seq", nullable = false)
    private Long serverSeq;

    @Column(name = "delivery_status", nullable = false, length = 32)
    private String deliveryStatus;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "failed_reason", length = 255)
    private String failedReason;

    @Column(name = "tencent_mirror_status", nullable = false, length = 32)
    private String tencentMirrorStatus;

    @Column(name = "tencent_message_key", length = 128)
    private String tencentMessageKey;

    @Column(name = "tencent_mirrored_at")
    private LocalDateTime tencentMirroredAt;

    @Column(name = "tencent_mirror_error", length = 255)
    private String tencentMirrorError;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SupportConversationEntity getConversation() {
        return conversation;
    }

    public void setConversation(SupportConversationEntity conversation) {
        this.conversation = conversation;
    }

    public UserEntity getSenderUser() {
        return senderUser;
    }

    public void setSenderUser(UserEntity senderUser) {
        this.senderUser = senderUser;
    }

    public String getSenderRole() {
        return senderRole;
    }

    public void setSenderRole(String senderRole) {
        this.senderRole = senderRole;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getClientMessageId() {
        return clientMessageId;
    }

    public void setClientMessageId(String clientMessageId) {
        this.clientMessageId = clientMessageId;
    }

    public Long getServerSeq() {
        return serverSeq;
    }

    public void setServerSeq(Long serverSeq) {
        this.serverSeq = serverSeq;
    }

    public String getDeliveryStatus() {
        return deliveryStatus;
    }

    public void setDeliveryStatus(String deliveryStatus) {
        this.deliveryStatus = deliveryStatus;
    }

    public LocalDateTime getDeliveredAt() {
        return deliveredAt;
    }

    public void setDeliveredAt(LocalDateTime deliveredAt) {
        this.deliveredAt = deliveredAt;
    }

    public String getFailedReason() {
        return failedReason;
    }

    public void setFailedReason(String failedReason) {
        this.failedReason = failedReason;
    }

    public String getTencentMirrorStatus() {
        return tencentMirrorStatus;
    }

    public void setTencentMirrorStatus(String tencentMirrorStatus) {
        this.tencentMirrorStatus = tencentMirrorStatus;
    }

    public String getTencentMessageKey() {
        return tencentMessageKey;
    }

    public void setTencentMessageKey(String tencentMessageKey) {
        this.tencentMessageKey = tencentMessageKey;
    }

    public LocalDateTime getTencentMirroredAt() {
        return tencentMirroredAt;
    }

    public void setTencentMirroredAt(LocalDateTime tencentMirroredAt) {
        this.tencentMirroredAt = tencentMirroredAt;
    }

    public String getTencentMirrorError() {
        return tencentMirrorError;
    }

    public void setTencentMirrorError(String tencentMirrorError) {
        this.tencentMirrorError = tencentMirrorError;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
