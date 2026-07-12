package com.cardnova.giftchat.service;

import com.cardnova.giftchat.dto.UpdateCurrencyExchangeRateRequest;
import com.cardnova.giftchat.entity.CurrencyExchangeRateEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.model.CurrencyExchangeRateItem;
import com.cardnova.giftchat.repository.CurrencyExchangeRateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CurrencyExchangeRateService {
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CurrencyExchangeRateRepository repository;
    private final CurrentUserService currentUserService;
    private final CountryCodeService countryCodeService;

    public CurrencyExchangeRateService(
        CurrencyExchangeRateRepository repository,
        CurrentUserService currentUserService,
        CountryCodeService countryCodeService
    ) {
        this.repository = repository;
        this.currentUserService = currentUserService;
        this.countryCodeService = countryCodeService;
    }

    public List<CurrencyExchangeRateItem> adminRates() {
        currentUserService.requireAdmin(currentUserService.getCurrentUser());
        return repository.findAllByOrderByCountryCodeAsc().stream().map(this::toItem).toList();
    }

    public CurrencyExchangeRateItem currentRate() {
        UserEntity user = currentUserService.getCurrentUser();
        if (!"USER".equalsIgnoreCase(user.getRoleCode()) || user.getCountryCode() == null) {
            throw new IllegalArgumentException("Current user has no bound country");
        }
        return toItem(requireEnabledRate(user.getCountryCode()));
    }

    public CurrencyExchangeRateEntity requireEnabledRate(String countryCode) {
        var country = countryCodeService.requireCountry(countryCode);
        CurrencyExchangeRateEntity entity = repository.findByCountryCode(country.code())
            .orElseThrow(() -> new IllegalArgumentException("Currency exchange rate is not configured"));
        if (!entity.isEnabled()) {
            throw new IllegalArgumentException("Currency exchange rate is not enabled");
        }
        return entity;
    }

    public BigDecimal convertUsd(String countryCode, BigDecimal usdAmount) {
        return usdAmount.multiply(requireEnabledRate(countryCode).getLocalCurrencyPerUsd())
            .setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional
    public CurrencyExchangeRateItem update(UpdateCurrencyExchangeRateRequest request) {
        UserEntity admin = currentUserService.getCurrentUser();
        currentUserService.requireAdmin(admin);
        var country = countryCodeService.requireCountry(request.countryCode());
        BigDecimal rate = request.localCurrencyPerUsd().setScale(6, RoundingMode.HALF_UP);
        if ("US".equals(country.code()) && rate.compareTo(BigDecimal.ONE) != 0) {
            throw new IllegalArgumentException("United States exchange rate must equal 1 USD");
        }
        CurrencyExchangeRateEntity entity = repository.findByCountryCode(country.code()).orElseGet(() -> {
            CurrencyExchangeRateEntity created = new CurrencyExchangeRateEntity();
            created.setId(UUID.randomUUID().toString());
            created.setCountryCode(country.code());
            created.setCreatedAt(LocalDateTime.now());
            return created;
        });
        entity.setCurrencyCode(country.currencyCode());
        entity.setLocalCurrencyPerUsd(rate);
        entity.setEnabled(request.enabled());
        entity.setNote(request.note() == null ? "" : request.note().trim());
        entity.setUpdatedBy(admin);
        entity.setUpdatedAt(LocalDateTime.now());
        return toItem(repository.save(entity));
    }

    public CurrencyExchangeRateItem toItem(CurrencyExchangeRateEntity entity) {
        var country = countryCodeService.requireCountry(entity.getCountryCode());
        String amount = entity.getLocalCurrencyPerUsd().stripTrailingZeros().toPlainString();
        String separator = country.currencySymbol().length() > 1 ? " " : "";
        return new CurrencyExchangeRateItem(
            entity.getId(),
            country.code(),
            country.countryName(),
            country.currencyCode(),
            country.currencySymbol(),
            amount,
            "$1 = " + country.currencySymbol() + separator + amount,
            entity.isEnabled(),
            entity.getNote() == null ? "" : entity.getNote(),
            TIME_FORMATTER.format(entity.getUpdatedAt()),
            entity.getUpdatedBy() == null ? "" : entity.getUpdatedBy().getUsername()
        );
    }
}
