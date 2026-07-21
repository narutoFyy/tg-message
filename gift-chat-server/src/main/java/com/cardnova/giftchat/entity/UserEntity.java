package com.cardnova.giftchat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.LocalDate;

@Entity
@Table(name = "app_user")
public class UserEntity {

    @Id
    private String id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(length = 128, unique = true)
    private String email;

    @Column(length = 32, unique = true)
    private String phone;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "role_code", nullable = false, length = 32)
    private String roleCode;

    @Column(name = "status_code", nullable = false, length = 32)
    private String statusCode;

    @Column(name = "tencent_user_id", length = 32, unique = true)
    private String tencentUserId;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Column(name = "invite_code", unique = true, length = 32)
    private String inviteCode;

    @Column(name = "referred_by_user_id", length = 36)
    private String referredByUserId;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column(name = "currency_code", length = 3)
    private String currencyCode;

    @Column(name = "country_binding_status", length = 16)
    private String countryBindingStatus;

    @Column(name = "country_bound_at")
    private LocalDateTime countryBoundAt;

    @Column(name = "country_bound_by", length = 36)
    private String countryBoundBy;

    @Column(name = "birth_date")
    private LocalDate birthDate;

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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(String statusCode) {
        this.statusCode = statusCode;
    }

    public String getTencentUserId() {
        return tencentUserId;
    }

    public void setTencentUserId(String tencentUserId) {
        this.tencentUserId = tencentUserId;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public String getReferredByUserId() {
        return referredByUserId;
    }

    public void setReferredByUserId(String referredByUserId) {
        this.referredByUserId = referredByUserId;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getCountryBindingStatus() {
        return countryBindingStatus;
    }

    public void setCountryBindingStatus(String countryBindingStatus) {
        this.countryBindingStatus = countryBindingStatus;
    }

    public LocalDateTime getCountryBoundAt() {
        return countryBoundAt;
    }

    public void setCountryBoundAt(LocalDateTime countryBoundAt) {
        this.countryBoundAt = countryBoundAt;
    }

    public String getCountryBoundBy() {
        return countryBoundBy;
    }

    public void setCountryBoundBy(String countryBoundBy) {
        this.countryBoundBy = countryBoundBy;
    }

    public LocalDate getBirthDate() { return birthDate; }
    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

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
