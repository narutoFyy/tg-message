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
@Table(name = "user_bank_account")
public class UserBankAccountEntity {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_id", nullable = false)
    private UserEntity ownerUser;

    @Column(nullable = false, length = 64)
    private String country;

    @Column(name = "account_name", nullable = false, length = 128)
    private String accountName;

    @Column(name = "bank_name", nullable = false, length = 128)
    private String bankName;

    @Column(name = "account_number", nullable = false, length = 128)
    private String accountNumber;

    @Column(name = "normalized_bank_name", nullable = false, length = 128)
    private String normalizedBankName;

    @Column(name = "normalized_account_number", nullable = false, length = 128)
    private String normalizedAccountNumber;

    @Column(name = "account_fingerprint", nullable = false, unique = true, length = 64)
    private String accountFingerprint;

    @Column(name = "masked_account_number", nullable = false, length = 64)
    private String maskedAccountNumber;

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

    public UserEntity getOwnerUser() {
        return ownerUser;
    }

    public void setOwnerUser(UserEntity ownerUser) {
        this.ownerUser = ownerUser;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getBankName() {
        return bankName;
    }

    public void setBankName(String bankName) {
        this.bankName = bankName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getNormalizedBankName() {
        return normalizedBankName;
    }

    public void setNormalizedBankName(String normalizedBankName) {
        this.normalizedBankName = normalizedBankName;
    }

    public String getNormalizedAccountNumber() {
        return normalizedAccountNumber;
    }

    public void setNormalizedAccountNumber(String normalizedAccountNumber) {
        this.normalizedAccountNumber = normalizedAccountNumber;
    }

    public String getAccountFingerprint() {
        return accountFingerprint;
    }

    public void setAccountFingerprint(String accountFingerprint) {
        this.accountFingerprint = accountFingerprint;
    }

    public String getMaskedAccountNumber() {
        return maskedAccountNumber;
    }

    public void setMaskedAccountNumber(String maskedAccountNumber) {
        this.maskedAccountNumber = maskedAccountNumber;
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
