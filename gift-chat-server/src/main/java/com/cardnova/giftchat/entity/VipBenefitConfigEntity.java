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
@Table(name = "vip_benefit_config")
public class VipBenefitConfigEntity {
    @Id private String id;
    @Column(name = "vip4_support_amount_ngn", nullable = false, precision = 18, scale = 2) private BigDecimal vip4SupportAmountNgn;
    @Column(name = "vip5_support_amount_ngn", nullable = false, precision = 18, scale = 2) private BigDecimal vip5SupportAmountNgn;
    @Column(name = "support_reward_enabled", nullable = false) private boolean supportRewardEnabled;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "updated_by") private UserEntity updatedBy;
    @Column(name = "created_at", nullable = false) private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private LocalDateTime updatedAt;
    public String getId() { return id; } public void setId(String id) { this.id = id; }
    public BigDecimal getVip4SupportAmountNgn() { return vip4SupportAmountNgn; } public void setVip4SupportAmountNgn(BigDecimal value) { this.vip4SupportAmountNgn = value; }
    public BigDecimal getVip5SupportAmountNgn() { return vip5SupportAmountNgn; } public void setVip5SupportAmountNgn(BigDecimal value) { this.vip5SupportAmountNgn = value; }
    public boolean isSupportRewardEnabled() { return supportRewardEnabled; } public void setSupportRewardEnabled(boolean value) { this.supportRewardEnabled = value; }
    public UserEntity getUpdatedBy() { return updatedBy; } public void setUpdatedBy(UserEntity value) { this.updatedBy = value; }
    public LocalDateTime getCreatedAt() { return createdAt; } public void setCreatedAt(LocalDateTime value) { this.createdAt = value; }
    public LocalDateTime getUpdatedAt() { return updatedAt; } public void setUpdatedAt(LocalDateTime value) { this.updatedAt = value; }
}
