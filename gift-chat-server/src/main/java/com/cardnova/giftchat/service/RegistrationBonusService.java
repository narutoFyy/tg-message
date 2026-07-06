package com.cardnova.giftchat.service;

import com.cardnova.giftchat.dto.UpdateRegistrationBonusConfigRequest;
import com.cardnova.giftchat.entity.RegistrationBonusConfigEntity;
import com.cardnova.giftchat.entity.RegistrationBonusRecordEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.model.RegistrationBonusConfigItem;
import com.cardnova.giftchat.model.RegistrationBonusRecordItem;
import com.cardnova.giftchat.repository.RegistrationBonusConfigRepository;
import com.cardnova.giftchat.repository.RegistrationBonusRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class RegistrationBonusService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");

    private final RegistrationBonusConfigRepository configRepository;
    private final RegistrationBonusRecordRepository recordRepository;
    private final CurrentUserService currentUserService;
    private final PhoneCountryCodeResolver phoneCountryCodeResolver;

    public RegistrationBonusService(
        RegistrationBonusConfigRepository configRepository,
        RegistrationBonusRecordRepository recordRepository,
        CurrentUserService currentUserService,
        PhoneCountryCodeResolver phoneCountryCodeResolver
    ) {
        this.configRepository = configRepository;
        this.recordRepository = recordRepository;
        this.currentUserService = currentUserService;
        this.phoneCountryCodeResolver = phoneCountryCodeResolver;
    }

    public List<RegistrationBonusConfigItem> configs() {
        currentUserService.requireAdmin(currentUserService.getCurrentUser());
        return configRepository.findAllByOrderByCountryCodeAsc().stream().map(this::toConfigItem).toList();
    }

    public List<RegistrationBonusRecordItem> records() {
        currentUserService.requireAdmin(currentUserService.getCurrentUser());
        return recordRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toRecordItem).toList();
    }

    @Transactional
    public RegistrationBonusConfigItem updateConfig(UpdateRegistrationBonusConfigRequest request) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        currentUserService.requireAdmin(currentUser);

        String countryCode = phoneCountryCodeResolver.normalizeCountryCode(request.countryCode());
        if (!StringUtils.hasText(countryCode)) {
            throw new IllegalArgumentException("Country code is required");
        }

        RegistrationBonusConfigEntity entity = configRepository.findByCountryCode(countryCode).orElseGet(() -> {
            RegistrationBonusConfigEntity created = new RegistrationBonusConfigEntity();
            created.setId(UUID.randomUUID().toString());
            created.setCountryCode(countryCode);
            created.setCreatedAt(LocalDateTime.now());
            return created;
        });
        entity.setCountryName(requireTrimmed(request.countryName(), "Country name is required"));
        entity.setCurrencyCode(requireTrimmed(request.currencyCode(), "Currency is required").toUpperCase());
        entity.setBonusAmount(request.bonusAmount().max(BigDecimal.ZERO).setScale(2, java.math.RoundingMode.HALF_UP));
        entity.setEnabled(request.enabled());
        entity.setNote(normalizeNullable(request.note()));
        entity.setUpdatedBy(currentUser);
        entity.setUpdatedAt(LocalDateTime.now());
        return toConfigItem(configRepository.save(entity));
    }

    @Transactional
    public void awardRegistrationBonus(UserEntity user) {
        if (user == null || recordRepository.existsByUser_Id(user.getId())) {
            return;
        }

        List<RegistrationBonusConfigEntity> configs = configRepository.findAllByOrderByCountryCodeAsc();
        String countryCode = phoneCountryCodeResolver.resolve(
            user.getPhone(),
            configs.stream().map(RegistrationBonusConfigEntity::getCountryCode).toList()
        );
        RegistrationBonusConfigEntity config = StringUtils.hasText(countryCode)
            ? configs.stream()
                .filter(item -> item.getCountryCode().equals(countryCode))
                .filter(RegistrationBonusConfigEntity::isEnabled)
                .findFirst()
                .orElse(null)
            : null;

        RegistrationBonusRecordEntity record = new RegistrationBonusRecordEntity();
        record.setId(UUID.randomUUID().toString());
        record.setUser(user);
        record.setPhoneSnapshot(user.getPhone());
        record.setCountryCode(countryCode);
        record.setConfig(config);
        record.setCreatedAt(LocalDateTime.now());
        if (config == null) {
            record.setCountryName("");
            record.setCurrencyCode("");
            record.setBonusAmount(BigDecimal.ZERO.setScale(2));
            record.setStatusCode("SKIPPED");
            record.setReasonNote(StringUtils.hasText(countryCode) ? "No enabled country bonus config" : "Phone country code not recognized");
        } else {
            record.setCountryName(config.getCountryName());
            record.setCurrencyCode(config.getCurrencyCode());
            record.setBonusAmount(config.getBonusAmount());
            record.setStatusCode(config.getBonusAmount().compareTo(BigDecimal.ZERO) > 0 ? "AVAILABLE" : "SKIPPED");
            record.setReasonNote("Country code registration bonus");
        }
        recordRepository.save(record);
    }

    public BigDecimal availableBonusesForUsers(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return recordRepository.findByUser_IdInAndStatusCode(userIds, "AVAILABLE").stream()
            .map(RegistrationBonusRecordEntity::getBonusAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public RegistrationBonusRecordItem recordForUser(String userId) {
        return recordRepository.findByUser_Id(userId).map(this::toRecordItem).orElse(null);
    }

    public RegistrationBonusRecordItem currentUserRecord() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        return recordForUser(currentUser.getId());
    }

    public List<String> configuredCountryCodes() {
        return configRepository.findAllByOrderByCountryCodeAsc().stream()
            .map(RegistrationBonusConfigEntity::getCountryCode)
            .toList();
    }

    private RegistrationBonusConfigItem toConfigItem(RegistrationBonusConfigEntity entity) {
        return new RegistrationBonusConfigItem(
            entity.getId(),
            entity.getCountryCode(),
            entity.getCountryName(),
            entity.getCurrencyCode(),
            money(entity.getBonusAmount()),
            entity.isEnabled(),
            value(entity.getNote()),
            TIME_FORMATTER.format(entity.getUpdatedAt()),
            entity.getUpdatedBy() == null ? "" : entity.getUpdatedBy().getUsername()
        );
    }

    private RegistrationBonusRecordItem toRecordItem(RegistrationBonusRecordEntity entity) {
        return new RegistrationBonusRecordItem(
            entity.getId(),
            entity.getUser().getUsername(),
            value(entity.getPhoneSnapshot()),
            value(entity.getCountryCode()),
            value(entity.getCountryName()),
            value(entity.getCurrencyCode()),
            money(entity.getBonusAmount()),
            entity.getStatusCode().toLowerCase(),
            value(entity.getReasonNote()),
            TIME_FORMATTER.format(entity.getCreatedAt())
        );
    }

    private String money(BigDecimal value) {
        return MONEY_FORMAT.format(value == null ? BigDecimal.ZERO : value);
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String requireTrimmed(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
