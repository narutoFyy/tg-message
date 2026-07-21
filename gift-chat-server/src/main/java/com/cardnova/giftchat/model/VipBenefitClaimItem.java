package com.cardnova.giftchat.model;
public record VipBenefitClaimItem(
    String id,
    String username,
    String benefitType,
    String periodKey,
    String vipLevel,
    String status,
    String baseAmountUsd,
    String localAmount,
    String currencyCode,
    String requestedAt,
    String reviewedBy,
    String reviewedAt,
    String reviewNote
) {}
