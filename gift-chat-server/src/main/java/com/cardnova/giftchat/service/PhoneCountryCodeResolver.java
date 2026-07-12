package com.cardnova.giftchat.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;

@Component
public class PhoneCountryCodeResolver {

    private static final List<String> FALLBACK_CODES = List.of(
        "+234", "+91", "+237", "+233", "+254", "+86", "+44", "+1"
    );

    public String resolve(String phone, Collection<String> configuredCodes) {
        if (!StringUtils.hasText(phone)) {
            return "";
        }
        String normalizedPhone = normalizePhone(phone);
        List<String> candidates = configuredCodes == null || configuredCodes.isEmpty()
            ? FALLBACK_CODES
            : configuredCodes.stream().filter(StringUtils::hasText).map(this::normalizeCountryCode).toList();

        return candidates.stream()
            .filter(StringUtils::hasText)
            .sorted(Comparator.comparingInt(String::length).reversed())
            .filter(normalizedPhone::startsWith)
            .findFirst()
            .orElseGet(() -> fallbackFromPhone(normalizedPhone));
    }

    public String normalizeCountryCode(String countryCode) {
        if (!StringUtils.hasText(countryCode)) {
            return "";
        }
        String digits = countryCode.trim().replaceAll("[^0-9]", "");
        return digits.isBlank() ? "" : "+" + digits;
    }

    private String normalizePhone(String phone) {
        String trimmed = phone.trim();
        if (trimmed.startsWith("00")) {
            trimmed = "+" + trimmed.substring(2);
        }
        return "+" + trimmed.replaceAll("[^0-9]", "");
    }

    private String fallbackFromPhone(String normalizedPhone) {
        return FALLBACK_CODES.stream()
            .filter(normalizedPhone::startsWith)
            .findFirst()
            .orElse("");
    }
}
