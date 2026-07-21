package com.cardnova.giftchat.dto;
import jakarta.validation.constraints.NotBlank;
public record ReviewVipBenefitClaimRequest(@NotBlank String status, String reviewNote) {}
