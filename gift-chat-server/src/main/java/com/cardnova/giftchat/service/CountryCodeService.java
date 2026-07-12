package com.cardnova.giftchat.service;

import com.cardnova.giftchat.model.CountryCodeRule;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class CountryCodeService {

    private static final List<CountryCodeRule> RULES = List.of(
        new CountryCodeRule("NG", "+234", "Nigeria", "NGN", "₦", 10, 10, true, 10),
        new CountryCodeRule("IN", "+91", "India", "INR", "₹", 10, 10, true, 20),
        new CountryCodeRule("CM", "+237", "Cameroon", "XAF", "FCFA", 9, 9, true, 30),
        new CountryCodeRule("GH", "+233", "Ghana", "GHS", "GH₵", 9, 9, true, 40),
        new CountryCodeRule("KE", "+254", "Kenya", "KES", "KSh", 9, 9, true, 50),
        new CountryCodeRule("US", "+1", "United States", "USD", "$", 10, 10, true, 60)
    );

    public List<CountryCodeRule> rules() {
        return RULES.stream()
            .sorted(Comparator.comparingInt(CountryCodeRule::sortOrder))
            .toList();
    }

    public CountryCodeRule requireCountry(String code) {
        String normalized = normalizeIsoCode(code);
        return RULES.stream()
            .filter(CountryCodeRule::enabled)
            .filter(rule -> rule.code().equals(normalized))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unsupported country"));
    }

    public Optional<CountryCodeRule> findCountry(String code) {
        String normalized = normalizeIsoCode(code);
        return RULES.stream().filter(rule -> rule.enabled() && rule.code().equals(normalized)).findFirst();
    }

    public Optional<CountryCodeRule> resolvePhoneCountry(String phone) {
        if (!StringUtils.hasText(phone)) {
            return Optional.empty();
        }
        String normalized = normalizeFullPhone(phone);
        return rules().stream()
            .filter(CountryCodeRule::enabled)
            .sorted(Comparator.comparingInt((CountryCodeRule rule) -> rule.countryCode().length()).reversed())
            .filter(rule -> normalized.startsWith(rule.countryCode()))
            .findFirst();
    }

    public String normalizeRegistrationPhone(String phone, String selectedCountryCode) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        CountryCodeRule selectedCountry = requireCountry(selectedCountryCode);
        String normalized = normalizeFullPhone(phone);
        CountryCodeRule rule = resolveRule(normalized);
        if (rule == null) {
            throw new IllegalArgumentException("Unsupported phone country code");
        }
        if (!rule.code().equals(selectedCountry.code())) {
            throw new IllegalArgumentException("Phone country code does not match selected country");
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

    private String normalizeIsoCode(String code) {
        return StringUtils.hasText(code) ? code.trim().toUpperCase(Locale.ROOT) : "";
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
