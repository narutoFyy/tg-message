package com.cardnova.giftchat.service;

import com.cardnova.giftchat.entity.LotteryDrawRecordEntity;
import com.cardnova.giftchat.entity.LotteryChanceEntity;
import com.cardnova.giftchat.entity.LotteryEligibilityResetEntity;
import com.cardnova.giftchat.entity.LotteryPrizeEntity;
import com.cardnova.giftchat.entity.CurrencyExchangeRateEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.model.LotteryDrawResult;
import com.cardnova.giftchat.model.LotteryEligibility;
import com.cardnova.giftchat.model.LotteryPrizeItem;
import com.cardnova.giftchat.model.LotteryRecordItem;
import com.cardnova.giftchat.model.LotteryWinnerItem;
import com.cardnova.giftchat.repository.LotteryDrawRecordRepository;
import com.cardnova.giftchat.repository.LotteryChanceRepository;
import com.cardnova.giftchat.repository.LotteryEligibilityResetRepository;
import com.cardnova.giftchat.repository.LotteryPrizeRepository;
import com.cardnova.giftchat.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class LotteryService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Set<String> FULFILLMENT_STATUSES = Set.of("PENDING", "PROCESSING", "FULFILLED", "CANCELED");
    private final LotteryPrizeRepository lotteryPrizeRepository;
    private final LotteryChanceRepository lotteryChanceRepository;
    private final LotteryDrawRecordRepository lotteryDrawRecordRepository;
    private final LotteryEligibilityResetRepository lotteryEligibilityResetRepository;
    private final CurrentUserService currentUserService;
    private final VipService vipService;
    private final UserRepository userRepository;
    private final CurrencyExchangeRateService currencyExchangeRateService;
    private final CountryCodeService countryCodeService;
    private final SecureRandom random = new SecureRandom();

    public LotteryService(
        LotteryPrizeRepository lotteryPrizeRepository,
        LotteryChanceRepository lotteryChanceRepository,
        LotteryDrawRecordRepository lotteryDrawRecordRepository,
        LotteryEligibilityResetRepository lotteryEligibilityResetRepository,
        CurrentUserService currentUserService,
        VipService vipService,
        UserRepository userRepository,
        CurrencyExchangeRateService currencyExchangeRateService,
        CountryCodeService countryCodeService
    ) {
        this.lotteryPrizeRepository = lotteryPrizeRepository;
        this.lotteryChanceRepository = lotteryChanceRepository;
        this.lotteryDrawRecordRepository = lotteryDrawRecordRepository;
        this.lotteryEligibilityResetRepository = lotteryEligibilityResetRepository;
        this.currentUserService = currentUserService;
        this.vipService = vipService;
        this.userRepository = userRepository;
        this.currencyExchangeRateService = currencyExchangeRateService;
        this.countryCodeService = countryCodeService;
    }

    @Transactional
    public LotteryEligibility currentEligibility() {
        UserEntity currentUser = lockUser(currentUserService.getCurrentUser().getId());
        return eligibilityFor(currentUser, LocalDateTime.now());
    }

    @Transactional
    public LotteryDrawResult spin() {
        UserEntity currentUser = lockUser(currentUserService.getCurrentUser().getId());
        LocalDateTime now = LocalDateTime.now();
        LotteryEligibility eligibility = eligibilityFor(currentUser, now);
        if (!eligibility.eligible()) {
            throw new IllegalArgumentException(eligibility.message());
        }

        LotteryChanceEntity chance = lotteryChanceRepository
            .findFirstByUser_IdAndConsumedAtIsNullOrderByGrantedAtAsc(currentUser.getId())
            .orElseThrow(() -> new IllegalArgumentException("No lottery chance is available."));
        LotteryPrizeEntity prize = choosePrize();
        LotteryDrawRecordEntity record = new LotteryDrawRecordEntity();
        record.setId(UUID.randomUUID().toString());
        record.setUser(currentUser);
        record.setVipLevel(eligibility.vipLevel());
        record.setPrize(prize);
        record.setLotteryChance(chance);
        record.setPeriodType(chance.getPeriodType());
        record.setPeriodKey(chance.getPeriodKey());
        record.setDrawnAt(now);
        record.setFulfillmentStatus("PENDING");
        if ("CASH".equalsIgnoreCase(prize.getPrizeType()) && prize.getBaseAmountUsd() != null) {
            CurrencyExchangeRateEntity rate = currencyExchangeRateService.requireEnabledRate(currentUser.getCountryCode());
            record.setBaseAmountUsd(prize.getBaseAmountUsd());
            record.setExchangeRateSnapshot(rate.getLocalCurrencyPerUsd());
            record.setLocalAmount(prize.getBaseAmountUsd().multiply(rate.getLocalCurrencyPerUsd()).setScale(2, RoundingMode.HALF_UP));
            record.setCurrencyCode(currentUser.getCurrencyCode());
        }

        try {
            LotteryDrawRecordEntity saved = lotteryDrawRecordRepository.saveAndFlush(record);
            chance.setConsumedAt(now);
            lotteryChanceRepository.save(chance);
            return new LotteryDrawResult(
                eligibilityFor(currentUser, now),
                toPrizeItem(prize, currentUser),
                saved.getId(),
                TIME_FORMATTER.format(saved.getDrawnAt())
            );
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("You have already used this lottery chance.");
        }
    }

    public List<LotteryWinnerItem> winners() {
        List<LotteryDrawRecordEntity> records = lotteryDrawRecordRepository.findTop50ByOrderByDrawnAtDesc();
        if (records.isEmpty()) {
            return demoWinners();
        }
        return records.stream()
            .map(record -> new LotteryWinnerItem(
                maskedUser(record.getUser()),
                record.getPrize().getName(),
                TIME_FORMATTER.format(record.getDrawnAt())
            ))
            .toList();
    }

    public List<LotteryPrizeItem> prizes() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        List<LotteryPrizeEntity> prizes = "ADMIN".equalsIgnoreCase(currentUser.getRoleCode())
            ? lotteryPrizeRepository.findAllByOrderBySortOrderAsc()
            : drawablePrizes();
        return prizes.stream()
            .map(prize -> toPrizeItem(prize, currentUser))
            .toList();
    }

    public List<LotteryRecordItem> adminRecords() {
        currentUserService.requireAdmin(currentUserService.getCurrentUser());
        return lotteryDrawRecordRepository.findAllByOrderByDrawnAtDesc().stream()
            .map(this::toRecordItem)
            .toList();
    }

    public List<LotteryRecordItem> myRecords() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        return lotteryDrawRecordRepository.findByUser_IdOrderByDrawnAtDesc(currentUser.getId()).stream()
            .map(this::toRecordItem)
            .toList();
    }

    @Transactional
    public LotteryRecordItem updateRecordStatus(String recordId, String status) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        currentUserService.requireAdmin(currentUser);
        String normalized = normalizeStatus(status);
        LotteryDrawRecordEntity record = lotteryDrawRecordRepository.findById(recordId)
            .orElseThrow(() -> new IllegalArgumentException("Lottery record not found"));
        record.setFulfillmentStatus(normalized);
        record.setProcessedBy(currentUser);
        record.setProcessedAt(LocalDateTime.now());
        return toRecordItem(lotteryDrawRecordRepository.save(record));
    }

    @Transactional
    public boolean resetUserEligibility(String userId, String reason) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        currentUserService.requireAdmin(currentUser);
        UserEntity targetUser = userRepository.findByIdForUpdate(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        LotteryEligibilityResetEntity reset = new LotteryEligibilityResetEntity();
        reset.setId(UUID.randomUUID().toString());
        reset.setUser(targetUser);
        reset.setAdminUser(currentUser);
        reset.setReason(reason == null ? "" : reason.trim());
        reset.setCreatedAt(LocalDateTime.now());
        lotteryEligibilityResetRepository.save(reset);
        grantChance(
            targetUser,
            "ADMIN_RESET:" + reset.getId(),
            "ADMIN_RESET",
            vipService.levelForUser(targetUser.getId()),
            "RESET",
            reset.getId().replace("-", "")
        );
        return true;
    }

    private LotteryEligibility eligibilityFor(UserEntity user, LocalDateTime now) {
        String vipLevel = vipService.levelForUser(user.getId());
        ensureChances(user, vipLevel, now);
        LotteryChanceEntity available = lotteryChanceRepository
            .findFirstByUser_IdAndConsumedAtIsNullOrderByGrantedAtAsc(user.getId())
            .orElse(null);
        long availableChances = lotteryChanceRepository.countByUser_IdAndConsumedAtIsNull(user.getId());
        Period currentPeriod = periodFor(vipLevel, now);
        String periodType = available == null
            ? currentPeriod == null ? "ONCE" : currentPeriod.type()
            : available.getPeriodType();
        String periodKey = available == null
            ? currentPeriod == null ? vipLevel : currentPeriod.key()
            : available.getPeriodKey();
        long drawCount = lotteryDrawRecordRepository.countByUser_IdAndPeriodTypeAndPeriodKey(user.getId(), periodType, periodKey);
        boolean eligible = availableChances > 0;
        String nextAvailableAt = eligible ? "" : nextAvailableAt(currentPeriod, now);
        String message = eligible
            ? availableChances + " lottery chance" + (availableChances == 1 ? "" : "s") + " available."
            : "Next chance available at " + nextAvailableAt;
        return new LotteryEligibility(
            vipLevel,
            eligible,
            periodType,
            periodKey,
            drawCount,
            availableChances,
            nextAvailableAt,
            message
        );
    }

    private void ensureChances(UserEntity user, String vipLevel, LocalDateTime now) {
        grantChance(user, "WELCOME:" + user.getId(), "WELCOME", "VIP0", "ONCE", "WELCOME");
        if (vipWeight(vipLevel) >= 1) {
            grantChance(user, "VIP1_UPGRADE:" + user.getId(), "VIP_UPGRADE", "VIP1", "ONCE", "VIP1-UPGRADE");
        }
        Period period = periodFor(vipLevel, now);
        if (period != null) {
            grantChance(
                user,
                "VIP_PERIOD:" + user.getId() + ":" + vipLevel + ":" + period.type() + ":" + period.key(),
                "VIP_PERIOD",
                vipLevel,
                period.type(),
                period.key()
            );
        }
        LotteryEligibilityResetEntity latestReset = lotteryEligibilityResetRepository.findFirstByUser_IdOrderByCreatedAtDesc(user.getId()).orElse(null);
        if (latestReset != null) {
            grantChance(
                user,
                "ADMIN_RESET:" + latestReset.getId(),
                "ADMIN_RESET",
                vipLevel,
                "RESET",
                latestReset.getId().replace("-", "")
            );
        }
    }

    private Period periodFor(String vipLevel, LocalDateTime now) {
        String normalized = vipLevel == null ? "VIP0" : vipLevel.toUpperCase(Locale.ROOT);
        if ("VIP2".equals(normalized)) {
            return new Period("MONTH", YearMonth.from(now).toString());
        }
        if ("VIP3".equals(normalized)) {
            String half = now.getDayOfMonth() <= 15 ? "H1" : "H2";
            return new Period("HALF_MONTH", YearMonth.from(now) + "-" + half);
        }
        if ("VIP4".equals(normalized) || "VIP5".equals(normalized)) {
            WeekFields weekFields = WeekFields.ISO;
            LocalDate date = now.toLocalDate();
            int week = date.get(weekFields.weekOfWeekBasedYear());
            int year = date.get(weekFields.weekBasedYear());
            return new Period("WEEK", "%04d-W%02d".formatted(year, week));
        }
        return null;
    }

    private String nextAvailableAt(Period basePeriod, LocalDateTime now) {
        if (basePeriod == null) {
            return "after the next VIP upgrade or admin reset";
        }
        if ("MONTH".equals(basePeriod.type())) {
            return TIME_FORMATTER.format(YearMonth.from(now).plusMonths(1).atDay(1).atStartOfDay());
        }
        if ("HALF_MONTH".equals(basePeriod.type())) {
            LocalDate next = now.getDayOfMonth() <= 15
                ? now.toLocalDate().withDayOfMonth(16)
                : YearMonth.from(now).plusMonths(1).atDay(1);
            return TIME_FORMATTER.format(next.atStartOfDay());
        }
        if ("WEEK".equals(basePeriod.type())) {
            LocalDate nextMonday = now.toLocalDate().with(DayOfWeek.MONDAY).plusWeeks(1);
            return TIME_FORMATTER.format(nextMonday.atStartOfDay());
        }
        return "after the next VIP upgrade or admin reset";
    }

    private void grantChance(
        UserEntity user,
        String sourceKey,
        String sourceType,
        String vipLevel,
        String periodType,
        String periodKey
    ) {
        if (lotteryChanceRepository.existsBySourceKey(sourceKey)) {
            return;
        }
        LotteryChanceEntity chance = new LotteryChanceEntity();
        chance.setId(UUID.randomUUID().toString());
        chance.setUser(user);
        chance.setSourceKey(sourceKey);
        chance.setSourceType(sourceType);
        chance.setVipLevel(vipLevel);
        chance.setPeriodType(periodType);
        chance.setPeriodKey(periodKey);
        chance.setGrantedAt(LocalDateTime.now());
        lotteryChanceRepository.save(chance);
    }

    private int vipWeight(String vipLevel) {
        if (vipLevel == null || !vipLevel.toUpperCase(Locale.ROOT).matches("VIP[0-5]")) {
            return 0;
        }
        return Integer.parseInt(vipLevel.substring(3));
    }

    private UserEntity lockUser(String userId) {
        return userRepository.findByIdForUpdate(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private LotteryPrizeEntity choosePrize() {
        List<LotteryPrizeEntity> prizes = drawablePrizes();
        if (prizes.isEmpty()) {
            throw new IllegalArgumentException("No drawable lottery prize is enabled");
        }
        int totalWeight = prizes.stream().mapToInt(prize -> Math.max(0, prize.getWeightValue())).sum();
        if (totalWeight <= 0) {
            return prizes.get(random.nextInt(prizes.size()));
        }
        int roll = random.nextInt(totalWeight);
        int cursor = 0;
        for (LotteryPrizeEntity prize : prizes) {
            cursor += Math.max(0, prize.getWeightValue());
            if (roll < cursor) {
                return prize;
            }
        }
        return prizes.get(prizes.size() - 1);
    }

    private List<LotteryPrizeEntity> drawablePrizes() {
        return lotteryPrizeRepository.findByEnabledTrueOrderBySortOrderAsc().stream()
            .filter(prize -> "PHYSICAL".equalsIgnoreCase(prize.getPrizeType())
                || ("CASH".equalsIgnoreCase(prize.getPrizeType()) && prize.getBaseAmountUsd() != null))
            .toList();
    }

    private LotteryPrizeItem toPrizeItem(LotteryPrizeEntity prize, UserEntity user) {
        MoneyView money = moneyView(prize, user);
        return new LotteryPrizeItem(
            prize.getId(),
            prize.getName(),
            prize.getPrizeType().toLowerCase(Locale.ROOT),
            money.baseAmountUsd(),
            money.localAmount(),
            money.currencyCode(),
            money.displayAmount(),
            money.exchangeRate(),
            prize.getWeightValue(),
            value(prize.getImageUrl()),
            prize.isEnabled(),
            prize.getSortOrder()
        );
    }

    private LotteryRecordItem toRecordItem(LotteryDrawRecordEntity record) {
        String displayAmount = displayAmount(record.getCurrencyCode(), record.getLocalAmount());
        return new LotteryRecordItem(
            record.getId(),
            record.getUser().getUsername(),
            record.getVipLevel(),
            record.getPrize().getName(),
            record.getPrize().getPrizeType().toLowerCase(Locale.ROOT),
            decimal(record.getBaseAmountUsd()),
            decimal(record.getLocalAmount()),
            value(record.getCurrencyCode()),
            displayAmount.isBlank() ? record.getPrize().getName() : displayAmount,
            decimal(record.getExchangeRateSnapshot()),
            record.getPeriodType().toLowerCase(Locale.ROOT),
            record.getPeriodKey(),
            record.getFulfillmentStatus().toLowerCase(Locale.ROOT),
            record.getProcessedBy() == null ? "" : record.getProcessedBy().getUsername(),
            record.getProcessedAt() == null ? "" : TIME_FORMATTER.format(record.getProcessedAt()),
            TIME_FORMATTER.format(record.getDrawnAt())
        );
    }

    private String normalizeStatus(String status) {
        if (!StringUtils.hasText(status)) {
            throw new IllegalArgumentException("Status is required");
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if (!FULFILLMENT_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported lottery status");
        }
        return normalized;
    }

    private String maskedUser(UserEntity user) {
        if (StringUtils.hasText(user.getPhone())) {
            return maskPhone(user.getPhone());
        }
        String username = user.getUsername();
        if (username.length() <= 2) {
            return username.substring(0, 1) + "***";
        }
        return username.substring(0, 2) + "***" + username.substring(username.length() - 1);
    }

    private String maskPhone(String phone) {
        String digits = phone.replaceAll("[^0-9]", "");
        if (digits.length() < 4) {
            return phone;
        }
        String last4 = digits.substring(digits.length() - 4);
        if (phone.startsWith("+234")) {
            return "+234 *** *** " + last4;
        }
        if (phone.startsWith("+233")) {
            return "+233 *** *** " + last4;
        }
        if (phone.startsWith("+91")) {
            return "+91 *** *** " + last4;
        }
        return "+*** *** " + last4;
    }

    private List<LotteryWinnerItem> demoWinners() {
        return List.of(
            new LotteryWinnerItem("+234 *** *** 4551", "₦1000", ""),
            new LotteryWinnerItem("+233 *** *** 7905", "₦800", ""),
            new LotteryWinnerItem("+234 *** *** 2866", "₦500", ""),
            new LotteryWinnerItem("+91 *** *** 1024", "₦200", "")
        );
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private MoneyView moneyView(LotteryPrizeEntity prize, UserEntity user) {
        if (!"CASH".equalsIgnoreCase(prize.getPrizeType()) || prize.getBaseAmountUsd() == null) {
            return new MoneyView("", "", "", prize.getName(), "");
        }
        String countryCode = "USER".equalsIgnoreCase(user.getRoleCode()) ? user.getCountryCode() : "US";
        CurrencyExchangeRateEntity rate = currencyExchangeRateService.requireEnabledRate(countryCode);
        BigDecimal localAmount = prize.getBaseAmountUsd().multiply(rate.getLocalCurrencyPerUsd()).setScale(2, RoundingMode.HALF_UP);
        String currencyCode = countryCodeService.requireCountry(countryCode).currencyCode();
        return new MoneyView(
            decimal(prize.getBaseAmountUsd()),
            decimal(localAmount),
            currencyCode,
            displayAmount(currencyCode, localAmount),
            decimal(rate.getLocalCurrencyPerUsd())
        );
    }

    private String displayAmount(String currencyCode, BigDecimal amount) {
        if (!StringUtils.hasText(currencyCode) || amount == null) {
            return "";
        }
        String symbol = countryCodeService.rules().stream()
            .filter(country -> currencyCode.equalsIgnoreCase(country.currencyCode()))
            .map(country -> country.currencySymbol())
            .findFirst()
            .orElse(currencyCode);
        String separator = symbol.length() > 1 ? " " : "";
        return symbol + separator + amount.stripTrailingZeros().toPlainString();
    }

    private String decimal(BigDecimal amount) {
        return amount == null ? "" : amount.stripTrailingZeros().toPlainString();
    }

    private record MoneyView(String baseAmountUsd, String localAmount, String currencyCode, String displayAmount, String exchangeRate) {
    }

    private record Period(String type, String key) {
    }
}
