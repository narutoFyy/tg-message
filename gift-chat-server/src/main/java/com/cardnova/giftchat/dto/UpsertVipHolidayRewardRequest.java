package com.cardnova.giftchat.dto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
public record UpsertVipHolidayRewardRequest(
    String id,
    @NotBlank String countryCode,
    @NotBlank String holidayCode,
    @NotBlank String holidayName,
    @NotNull LocalDate holidayDate,
    @NotNull @DecimalMin("0.00") BigDecimal rewardAmount,
    boolean enabled
) {}
