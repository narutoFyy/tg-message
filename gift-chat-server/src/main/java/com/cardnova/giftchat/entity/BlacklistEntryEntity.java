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
@Table(name = "blacklist_entry")
public class BlacklistEntryEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private UserEntity ownerUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_user_id", nullable = false)
    private UserEntity blockedUser;

    @Column(name = "blocked_phone_snapshot", nullable = false, length = 32)
    private String blockedPhoneSnapshot;

    @Column(name = "reason_note", length = 255)
    private String reasonNote;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UserEntity getOwnerUser() {
        return ownerUser;
    }

    public void setOwnerUser(UserEntity ownerUser) {
        this.ownerUser = ownerUser;
    }

    public UserEntity getBlockedUser() {
        return blockedUser;
    }

    public void setBlockedUser(UserEntity blockedUser) {
        this.blockedUser = blockedUser;
    }

    public String getBlockedPhoneSnapshot() {
        return blockedPhoneSnapshot;
    }

    public void setBlockedPhoneSnapshot(String blockedPhoneSnapshot) {
        this.blockedPhoneSnapshot = blockedPhoneSnapshot;
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
}
