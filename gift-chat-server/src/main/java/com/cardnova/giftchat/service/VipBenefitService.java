package com.cardnova.giftchat.service;

import com.cardnova.giftchat.dto.ReviewVipBenefitClaimRequest;
import com.cardnova.giftchat.dto.UpdateVipBenefitConfigRequest;
import com.cardnova.giftchat.dto.UpsertVipHolidayRewardRequest;
import com.cardnova.giftchat.entity.CurrencyExchangeRateEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.entity.VipBenefitClaimEntity;
import com.cardnova.giftchat.entity.VipBenefitConfigEntity;
import com.cardnova.giftchat.entity.VipHolidayRewardEntity;
import com.cardnova.giftchat.model.VipBenefitClaimItem;
import com.cardnova.giftchat.model.VipBenefitConfigItem;
import com.cardnova.giftchat.model.VipBenefitSummary;
import com.cardnova.giftchat.model.VipHolidayRewardItem;
import com.cardnova.giftchat.repository.SupportConversationRepository;
import com.cardnova.giftchat.repository.UserRepository;
import com.cardnova.giftchat.repository.VipBenefitClaimRepository;
import com.cardnova.giftchat.repository.VipBenefitConfigRepository;
import com.cardnova.giftchat.repository.VipHolidayRewardRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class VipBenefitService {

    private static final String CONFIG_ID = "vip-benefit-default";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Set<String> REVIEW_STATUSES = Set.of("APPROVED", "REJECTED");

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final VipService vipService;
    private final CurrencyExchangeRateService currencyExchangeRateService;
    private final CountryCodeService countryCodeService;
    private final SupportConversationRepository supportConversationRepository;
    private final VipBenefitConfigRepository configRepository;
    private final VipHolidayRewardRepository holidayRepository;
    private final VipBenefitClaimRepository claimRepository;

    public VipBenefitService(
        CurrentUserService currentUserService,
        UserRepository userRepository,
        VipService vipService,
        CurrencyExchangeRateService currencyExchangeRateService,
        CountryCodeService countryCodeService,
        SupportConversationRepository supportConversationRepository,
        VipBenefitConfigRepository configRepository,
        VipHolidayRewardRepository holidayRepository,
        VipBenefitClaimRepository claimRepository
    ) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.vipService = vipService;
        this.currencyExchangeRateService = currencyExchangeRateService;
        this.countryCodeService = countryCodeService;
        this.supportConversationRepository = supportConversationRepository;
        this.configRepository = configRepository;
        this.holidayRepository = holidayRepository;
        this.claimRepository = claimRepository;
    }

    public VipBenefitSummary currentSummary() {
        return summary(currentUserService.getCurrentUser(), LocalDate.now());
    }

    @Transactional
    public VipBenefitSummary setCurrentBirthday(LocalDate birthDate) {
        UserEntity current = currentUserService.getCurrentUser();
        if (!"USER".equalsIgnoreCase(current.getRoleCode())) {
            throw new IllegalArgumentException("Only users can set a birthday");
        }
        UserEntity user = lockUser(current.getId());
        if (user.getBirthDate() != null) {
            throw new IllegalArgumentException("Birthday is already locked. Contact support to change it.");
        }
        setBirthday(user, birthDate);
        return summary(user, LocalDate.now());
    }

    @Transactional
    public VipBenefitSummary setUserBirthdayByAdmin(String userId, LocalDate birthDate) {
        currentUserService.requireAdmin(currentUserService.getCurrentUser());
        UserEntity user = lockUser(userId);
        setBirthday(user, birthDate);
        return summary(user, LocalDate.now());
    }

    @Transactional
    public VipBenefitClaimItem claimBirthday() {
        UserEntity user = lockCurrentUser();
        String level = vipService.levelForUser(user.getId());
        requireVip(level, 2, "Birthday rewards require VIP2 or above");
        LocalDate today = LocalDate.now();
        if (user.getBirthDate() == null) {
            throw new IllegalArgumentException("Set your birthday first");
        }
        if (user.getBirthDate().getMonthValue() != today.getMonthValue()
            || user.getBirthDate().getDayOfMonth() != today.getDayOfMonth()) {
            throw new IllegalArgumentException("Birthday reward is only available on your birthday");
        }
        String sourceKey = "BIRTHDAY:" + user.getId() + ":" + today.getYear();
        requireUnclaimed(sourceKey, "Birthday reward has already been claimed this year");
        BigDecimal rewardNgn = birthdayRewardNgn(level);
        return toClaimItem(saveClaim(user, null, "BIRTHDAY", sourceKey, String.valueOf(today.getYear()), level, "APPROVED", fromNgn(user, rewardNgn)));
    }

    @Transactional
    public VipBenefitClaimItem requestSupportRedPacket() {
        UserEntity user = lockCurrentUser();
        String level = vipService.levelForUser(user.getId());
        requireVip(level, 4, "Support red packets require VIP4 or above");
        VipBenefitConfigEntity config = config();
        if (!config.isSupportRewardEnabled()) {
            throw new IllegalArgumentException("Support red packets are not enabled");
        }
        BigDecimal amountNgn = "VIP5".equals(level) ? config.getVip5SupportAmountNgn() : config.getVip4SupportAmountNgn();
        if (amountNgn == null || amountNgn.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Support red packet amount is not configured");
        }
        String period = YearMonth.now().toString();
        String sourceKey = "SUPPORT_RED_PACKET:" + user.getId() + ":" + period;
        requireUnclaimed(sourceKey, "Support red packet has already been requested this month");
        return toClaimItem(saveClaim(user, null, "SUPPORT_RED_PACKET", sourceKey, period, level, "PENDING", fromNgn(user, amountNgn)));
    }

    @Transactional
    public VipBenefitClaimItem claimHoliday(String holidayId) {
        UserEntity user = lockCurrentUser();
        String level = vipService.levelForUser(user.getId());
        requireVip(level, 5, "Holiday rewards require VIP5");
        VipHolidayRewardEntity holiday = holidayRepository.findById(holidayId)
            .orElseThrow(() -> new IllegalArgumentException("Holiday reward not found"));
        if (!holiday.isEnabled() || !holiday.getHolidayDate().equals(LocalDate.now())) {
            throw new IllegalArgumentException("Holiday reward is not available today");
        }
        if (!holiday.getCountryCode().equalsIgnoreCase(user.getCountryCode())) {
            throw new IllegalArgumentException("Holiday reward does not match your country");
        }
        String sourceKey = "HOLIDAY:" + user.getId() + ":" + holiday.getId();
        requireUnclaimed(sourceKey, "Holiday reward has already been claimed");
        MoneySnapshot money = fromLocal(user, holiday.getRewardAmount());
        return toClaimItem(saveClaim(user, holiday, "HOLIDAY", sourceKey, holiday.getHolidayDate().toString(), level, "APPROVED", money));
    }

    public List<VipBenefitClaimItem> currentUserClaims() {
        UserEntity user = currentUserService.getCurrentUser();
        return claimRepository.findByUser_IdOrderByRequestedAtDesc(user.getId()).stream().map(this::toClaimItem).toList();
    }

    public List<VipBenefitClaimItem> staffClaims() {
        UserEntity staff = currentUserService.getCurrentUser();
        currentUserService.requireAgentOrAdmin(staff);
        if ("ADMIN".equalsIgnoreCase(staff.getRoleCode())) {
            return claimRepository.findAllByOrderByRequestedAtDesc().stream().map(this::toClaimItem).toList();
        }
        List<String> customerIds = supportConversationRepository.findByAssignedAgent_IdOrderByUpdatedAtDesc(staff.getId()).stream()
            .map(item -> item.getCustomerUser().getId()).distinct().toList();
        return claimRepository.findAllByOrderByRequestedAtDesc().stream()
            .filter(item -> customerIds.contains(item.getUser().getId()))
            .map(this::toClaimItem)
            .toList();
    }

    @Transactional
    public VipBenefitClaimItem reviewClaim(String claimId, ReviewVipBenefitClaimRequest request) {
        UserEntity staff = currentUserService.getCurrentUser();
        currentUserService.requireAgentOrAdmin(staff);
        String status = request.status().trim().toUpperCase(Locale.ROOT);
        if (!REVIEW_STATUSES.contains(status)) {
            throw new IllegalArgumentException("Unsupported benefit claim status");
        }
        VipBenefitClaimEntity claim = claimRepository.findById(claimId)
            .orElseThrow(() -> new IllegalArgumentException("Benefit claim not found"));
        if (!"SUPPORT_RED_PACKET".equals(claim.getBenefitType()) || !"PENDING".equals(claim.getStatusCode())) {
            throw new IllegalArgumentException("Benefit claim cannot be reviewed");
        }
        if ("AGENT".equalsIgnoreCase(staff.getRoleCode())
            && !supportConversationRepository.existsByCustomerUser_IdAndAssignedAgent_Id(claim.getUser().getId(), staff.getId())) {
            throw new IllegalArgumentException("Benefit claim is not assigned to this agent");
        }
        claim.setStatusCode(status);
        claim.setReviewedBy(staff);
        claim.setReviewedAt(LocalDateTime.now());
        claim.setReviewNote(normalize(request.reviewNote()));
        return toClaimItem(claimRepository.save(claim));
    }

    public VipBenefitConfigItem adminConfig() {
        currentUserService.requireAdmin(currentUserService.getCurrentUser());
        return toConfigItem(config());
    }

    @Transactional
    public VipBenefitConfigItem updateConfig(UpdateVipBenefitConfigRequest request) {
        UserEntity admin = currentUserService.getCurrentUser();
        currentUserService.requireAdmin(admin);
        VipBenefitConfigEntity config = config();
        config.setVip4SupportAmountNgn(money(request.vip4SupportAmountNgn()));
        config.setVip5SupportAmountNgn(money(request.vip5SupportAmountNgn()));
        config.setSupportRewardEnabled(request.supportRewardEnabled());
        config.setUpdatedBy(admin);
        config.setUpdatedAt(LocalDateTime.now());
        return toConfigItem(configRepository.save(config));
    }

    public List<VipHolidayRewardItem> adminHolidays() {
        currentUserService.requireAdmin(currentUserService.getCurrentUser());
        return holidayRepository.findAllByOrderByHolidayDateDesc().stream().map(item -> toHolidayItem(item, null)).toList();
    }

    @Transactional
    public VipHolidayRewardItem upsertHoliday(UpsertVipHolidayRewardRequest request) {
        UserEntity admin = currentUserService.getCurrentUser();
        currentUserService.requireAdmin(admin);
        var country = countryCodeService.requireCountry(request.countryCode());
        String code = requireText(request.holidayCode(), "Holiday code is required").toUpperCase(Locale.ROOT);
        VipHolidayRewardEntity entity = StringUtils.hasText(request.id())
            ? holidayRepository.findById(request.id()).orElseThrow(() -> new IllegalArgumentException("Holiday reward not found"))
            : holidayRepository.findByCountryCodeAndHolidayCodeAndHolidayDate(country.code(), code, request.holidayDate()).orElseGet(() -> {
                VipHolidayRewardEntity created = new VipHolidayRewardEntity();
                created.setId(UUID.randomUUID().toString());
                created.setCreatedAt(LocalDateTime.now());
                return created;
            });
        entity.setCountryCode(country.code());
        entity.setHolidayCode(code);
        entity.setHolidayName(requireText(request.holidayName(), "Holiday name is required"));
        entity.setHolidayDate(request.holidayDate());
        entity.setRewardAmount(money(request.rewardAmount()));
        entity.setCurrencyCode(country.currencyCode());
        entity.setEnabled(request.enabled());
        entity.setUpdatedBy(admin);
        entity.setUpdatedAt(LocalDateTime.now());
        return toHolidayItem(holidayRepository.save(entity), null);
    }

    public BigDecimal availableCreditsForUsers(Collection<String> userIds) {
        if (userIds == null || userIds.isEmpty()) return BigDecimal.ZERO;
        return claimRepository.findByUser_IdInAndStatusCode(userIds, "APPROVED").stream()
            .map(VipBenefitClaimEntity::getLocalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private VipBenefitSummary summary(UserEntity user, LocalDate today) {
        String level = vipService.levelForUser(user.getId());
        String birthdayKey = "BIRTHDAY:" + user.getId() + ":" + today.getYear();
        boolean birthdayToday = user.getBirthDate() != null
            && user.getBirthDate().getMonthValue() == today.getMonthValue()
            && user.getBirthDate().getDayOfMonth() == today.getDayOfMonth();
        BigDecimal birthdayNgn = vipWeight(level) >= 2 ? birthdayRewardNgn(level) : BigDecimal.ZERO;
        String birthdayDisplay = birthdayNgn.signum() > 0 ? display(fromNgn(user, birthdayNgn)) : "";
        VipBenefitConfigEntity config = config();
        BigDecimal supportNgn = "VIP5".equals(level) ? config.getVip5SupportAmountNgn() : config.getVip4SupportAmountNgn();
        String supportKey = "SUPPORT_RED_PACKET:" + user.getId() + ":" + YearMonth.from(today);
        boolean supportEligible = vipWeight(level) >= 4 && config.isSupportRewardEnabled()
            && supportNgn != null && supportNgn.signum() > 0 && !claimRepository.existsBySourceKey(supportKey);
        List<VipHolidayRewardItem> holidays = vipWeight(level) >= 5
            ? holidayRepository.findByCountryCodeAndHolidayDateAndEnabledTrueOrderByHolidayNameAsc(user.getCountryCode(), today).stream()
                .map(item -> toHolidayItem(item, user)).toList()
            : List.of();
        return new VipBenefitSummary(
            level,
            user.getBirthDate() == null ? "" : user.getBirthDate().toString(),
            user.getBirthDate() != null,
            vipWeight(level) >= 2 && birthdayToday && !claimRepository.existsBySourceKey(birthdayKey),
            birthdayNgn.stripTrailingZeros().toPlainString(),
            birthdayDisplay,
            supportEligible,
            supportNgn == null || supportNgn.signum() <= 0 ? "" : display(fromNgn(user, supportNgn)),
            holidays
        );
    }

    private VipBenefitClaimEntity saveClaim(UserEntity user, VipHolidayRewardEntity holiday, String type, String sourceKey, String period, String level, String status, MoneySnapshot money) {
        VipBenefitClaimEntity claim = new VipBenefitClaimEntity();
        claim.setId(UUID.randomUUID().toString());
        claim.setUser(user);
        claim.setHolidayReward(holiday);
        claim.setBenefitType(type);
        claim.setSourceKey(sourceKey);
        claim.setPeriodKey(period);
        claim.setVipLevel(level);
        claim.setStatusCode(status);
        claim.setBaseAmountUsd(money.baseAmountUsd());
        claim.setLocalAmount(money.localAmount());
        claim.setCurrencyCode(money.currencyCode());
        claim.setExchangeRateSnapshot(money.exchangeRate());
        claim.setRequestedAt(LocalDateTime.now());
        return claimRepository.save(claim);
    }

    private MoneySnapshot fromNgn(UserEntity user, BigDecimal amountNgn) {
        CurrencyExchangeRateEntity ngnRate = currencyExchangeRateService.requireEnabledRate("NG");
        BigDecimal baseUsd = amountNgn.divide(ngnRate.getLocalCurrencyPerUsd(), 6, RoundingMode.HALF_UP);
        return fromUsd(user, baseUsd);
    }

    private MoneySnapshot fromLocal(UserEntity user, BigDecimal localAmount) {
        CurrencyExchangeRateEntity rate = currencyExchangeRateService.requireEnabledRate(user.getCountryCode());
        BigDecimal baseUsd = localAmount.divide(rate.getLocalCurrencyPerUsd(), 6, RoundingMode.HALF_UP);
        return new MoneySnapshot(baseUsd, money(localAmount), user.getCurrencyCode(), rate.getLocalCurrencyPerUsd());
    }

    private MoneySnapshot fromUsd(UserEntity user, BigDecimal baseUsd) {
        CurrencyExchangeRateEntity rate = currencyExchangeRateService.requireEnabledRate(user.getCountryCode());
        BigDecimal local = baseUsd.multiply(rate.getLocalCurrencyPerUsd()).setScale(2, RoundingMode.HALF_UP);
        return new MoneySnapshot(baseUsd.setScale(6, RoundingMode.HALF_UP), local, user.getCurrencyCode(), rate.getLocalCurrencyPerUsd());
    }

    private void setBirthday(UserEntity user, LocalDate birthDate) {
        if (birthDate == null || !birthDate.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Birthday must be in the past");
        }
        user.setBirthDate(birthDate);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private UserEntity lockCurrentUser() {
        UserEntity current = currentUserService.getCurrentUser();
        if (!"USER".equalsIgnoreCase(current.getRoleCode())) throw new IllegalArgumentException("Only users can claim VIP benefits");
        return lockUser(current.getId());
    }

    private UserEntity lockUser(String userId) {
        return userRepository.findByIdForUpdate(userId).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private VipBenefitConfigEntity config() {
        return configRepository.findById(CONFIG_ID).orElseThrow(() -> new IllegalArgumentException("VIP benefit config is missing"));
    }

    private void requireUnclaimed(String sourceKey, String message) {
        if (claimRepository.existsBySourceKey(sourceKey)) throw new IllegalArgumentException(message);
    }

    private void requireVip(String level, int required, String message) {
        if (vipWeight(level) < required) throw new IllegalArgumentException(message);
    }

    private int vipWeight(String level) {
        if (level == null || !level.toUpperCase(Locale.ROOT).matches("VIP[0-5]")) return 0;
        return Integer.parseInt(level.substring(3));
    }

    private BigDecimal birthdayRewardNgn(String level) {
        return switch (level) {
            case "VIP2" -> new BigDecimal("5000");
            case "VIP3" -> new BigDecimal("10000");
            case "VIP4" -> new BigDecimal("20000");
            case "VIP5" -> new BigDecimal("100000");
            default -> BigDecimal.ZERO;
        };
    }

    private VipBenefitConfigItem toConfigItem(VipBenefitConfigEntity entity) {
        return new VipBenefitConfigItem(decimal(entity.getVip4SupportAmountNgn()), decimal(entity.getVip5SupportAmountNgn()), entity.isSupportRewardEnabled(), TIME_FORMATTER.format(entity.getUpdatedAt()), entity.getUpdatedBy() == null ? "" : entity.getUpdatedBy().getUsername());
    }

    private VipHolidayRewardItem toHolidayItem(VipHolidayRewardEntity entity, UserEntity user) {
        boolean claimed = user != null && claimRepository.existsBySourceKey("HOLIDAY:" + user.getId() + ":" + entity.getId());
        boolean claimable = user != null && !claimed && entity.isEnabled() && entity.getHolidayDate().equals(LocalDate.now()) && "VIP5".equals(vipService.levelForUser(user.getId()));
        return new VipHolidayRewardItem(entity.getId(), entity.getCountryCode(), entity.getHolidayCode(), entity.getHolidayName(), entity.getHolidayDate().toString(), decimal(entity.getRewardAmount()), entity.getCurrencyCode(), entity.isEnabled(), claimable, claimed, TIME_FORMATTER.format(entity.getUpdatedAt()), entity.getUpdatedBy() == null ? "" : entity.getUpdatedBy().getUsername());
    }

    private VipBenefitClaimItem toClaimItem(VipBenefitClaimEntity entity) {
        return new VipBenefitClaimItem(entity.getId(), entity.getUser().getUsername(), entity.getBenefitType().toLowerCase(Locale.ROOT), entity.getPeriodKey(), entity.getVipLevel(), entity.getStatusCode().toLowerCase(Locale.ROOT), decimal(entity.getBaseAmountUsd()), decimal(entity.getLocalAmount()), entity.getCurrencyCode(), TIME_FORMATTER.format(entity.getRequestedAt()), entity.getReviewedBy() == null ? "" : entity.getReviewedBy().getUsername(), entity.getReviewedAt() == null ? "" : TIME_FORMATTER.format(entity.getReviewedAt()), value(entity.getReviewNote()));
    }

    private String display(MoneySnapshot money) {
        String symbol = countryCodeService.rules().stream().filter(item -> money.currencyCode().equals(item.currencyCode())).map(item -> item.currencySymbol()).findFirst().orElse(money.currencyCode());
        return symbol + (symbol.length() > 1 ? " " : "") + decimal(money.localAmount());
    }

    private BigDecimal money(BigDecimal value) { return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP); }
    private String decimal(BigDecimal value) { return value == null ? "0" : value.stripTrailingZeros().toPlainString(); }
    private String value(String value) { return value == null ? "" : value; }
    private String normalize(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
    private String requireText(String value, String message) { if (!StringUtils.hasText(value)) throw new IllegalArgumentException(message); return value.trim(); }
    private record MoneySnapshot(BigDecimal baseAmountUsd, BigDecimal localAmount, String currencyCode, BigDecimal exchangeRate) {}
}
