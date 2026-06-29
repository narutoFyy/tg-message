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
@Table(name = "friendship")
public class FriendshipEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_user_id", nullable = false)
    private UserEntity requesterUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "addressee_user_id", nullable = false)
    private UserEntity addresseeUser;

    @Column(name = "status_code", nullable = false, length = 32)
    private String statusCode;

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

    public UserEntity getRequesterUser() {
        return requesterUser;
    }

    public void setRequesterUser(UserEntity requesterUser) {
        this.requesterUser = requesterUser;
    }

    public UserEntity getAddresseeUser() {
        return addresseeUser;
    }

    public void setAddresseeUser(UserEntity addresseeUser) {
        this.addresseeUser = addresseeUser;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
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
