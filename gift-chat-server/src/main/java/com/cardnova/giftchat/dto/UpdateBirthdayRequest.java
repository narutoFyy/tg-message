package com.cardnova.giftchat.dto;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
public record UpdateBirthdayRequest(@NotNull LocalDate birthDate) {}
