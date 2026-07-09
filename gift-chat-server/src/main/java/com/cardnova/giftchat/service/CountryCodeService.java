package com.cardnova.giftchat.service;

import com.cardnova.giftchat.model.CountryCodeRule;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class CountryCodeService {

    private static final List<CountryCodeRule> RULES = List.of(
        new CountryCodeRule("+234", "Nigeria", 10, 10, true, 10),
        new CountryCodeRule("+91", "India", 10, 10, true, 20),
        new CountryCodeRule("+233", "Ghana", 9, 9, true, 30),
        new CountryCodeRule("+86", "China", 11, 11, true, 40),
        new CountryCodeRule("+44", "United Kingdom", 10, 10, true, 50),
        new CountryCodeRule("+1", "United States", 10, 10, true, 60)
    );

    public List<CountryCodeRule> rules() {
        return RULES.stream()
            .sorted(Comparator.comparingInt(CountryCodeRule::sortOrder))
            .toList();
    }

    public String normalizeRegistrationPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        String normalized = normalizeFullPhone(phone);
        CountryCodeRule rule = resolveRule(normalized);
        if (rule == null) {
            throw new IllegalArgumentException("Unsupported phone country code");
        }
        String localNumber = normalized.substring(rule.countryCode().length()).replaceAll("[^0-9]", "");
        if (localNumber.length() < rule.minLocalLength() || localNumber.length() > rule.maxLocalLength()) {
            String lengthText = rule.minLocalLength() == rule.maxLocalLength()
                ? "%d digits".formatted(rule.minLocalLength())
                : "%d-%d digits".formatted(rule.minLocalLength(), rule.maxLocalLength());
            throw new IllegalArgumentException("%s phone number must be %s".formatted(rule.countryName(), lengthText));
        }
        return rule.countryCode() + localNumber;
    }

    private CountryCodeRule resolveRule(String normalizedPhone) {
        return rules().stream()
            .filter(CountryCodeRule::enabled)
            .sorted(Comparator.comparingInt((CountryCodeRule rule) -> rule.countryCode().length()).reversed())
            .filter(rule -> normalizedPhone.startsWith(rule.countryCode()))
            .findFirst()
            .orElse(null);
    }

    private String normalizeFullPhone(String phone) {
        String trimmed = phone.trim();
        if (trimmed.startsWith("00")) {
            trimmed = "+" + trimmed.substring(2);
        }
        String digits = trimmed.replaceAll("[^0-9]", "");
        if (!StringUtils.hasText(digits)) {
            throw new IllegalArgumentException("Phone is required");
        }
        if (!trimmed.startsWith("+") && !trimmed.startsWith("00")) {
            throw new IllegalArgumentException("Phone country code is required");
        }
        return "+" + digits.toLowerCase(Locale.ROOT);
    }
}
