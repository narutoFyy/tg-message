package com.cardnova.giftchat.service;

import com.cardnova.giftchat.entity.LotteryDrawRecordEntity;
import com.cardnova.giftchat.entity.LotteryEligibilityResetEntity;
import com.cardnova.giftchat.entity.LotteryPrizeEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.model.LotteryDrawResult;
import com.cardnova.giftchat.model.LotteryEligibility;
import com.cardnova.giftchat.model.LotteryPrizeItem;
import com.cardnova.giftchat.model.LotteryRecordItem;
import com.cardnova.giftchat.model.LotteryWinnerItem;
import com.cardnova.giftchat.repository.LotteryDrawRecordRepository;
import com.cardnova.giftchat.repository.LotteryEligibilityResetRepository;
import com.cardnova.giftchat.repository.LotteryPrizeRepository;
import com.cardnova.giftchat.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
    private static final DateTimeFormatter DAY_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final Set<String> FULFILLMENT_STATUSES = Set.of("PENDING", "PROCESSING", "FULFILLED", "CANCELED");
    private static final Set<String> DRAWABLE_PRIZE_NAMES = Set.of("₦1000", "₦2000", "₦3000", "₦5000");

    private final LotteryPrizeRepository lotteryPrizeRepository;
    private final LotteryDrawRecordRepository lotteryDrawRecordRepository;
    private final LotteryEligibilityResetRepository lotteryEligibilityResetRepository;
    private final CurrentUserService currentUserService;
    private final VipService vipService;
    private final UserRepository userRepository;
    private final SecureRandom random = new SecureRandom();

    public LotteryService(
        LotteryPrizeRepository lotteryPrizeRepository,
        LotteryDrawRecordRepository lotteryDrawRecordRepository,
        LotteryEligibilityResetRepository lotteryEligibilityResetRepository,
        CurrentUserService currentUserService,
        VipService vipService,
        UserRepository userRepository
    ) {
        this.lotteryPrizeRepository = lotteryPrizeRepository;
        this.lotteryDrawRecordRepository = lotteryDrawRecordRepository;
        this.lotteryEligibilityResetRepository = lotteryEligibilityResetRepository;
        this.currentUserService = currentUserService;
        this.vipService = vipService;
        this.userRepository = userRepository;
    }

    public LotteryEligibility currentEligibility() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        return eligibilityFor(currentUser, LocalDateTime.now());
    }

    @Transactional
    public LotteryDrawResult spin(String requestedPrizeName) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        LocalDateTime now = LocalDateTime.now();
        LotteryEligibility eligibility = eligibilityFor(currentUser, now);
        if (!eligibility.eligible()) {
            throw new IllegalArgumentException(eligibility.message());
        }

        LotteryPrizeEntity prize = choosePrize(requestedPrizeName);
        LotteryDrawRecordEntity record = new LotteryDrawRecordEntity();
        record.setId(UUID.randomUUID().toString());
        record.setUser(currentUser);
        record.setVipLevel(eligibility.vipLevel());
        record.setPrize(prize);
        record.setPeriodType(eligibility.periodType());
        record.setPeriodKey(eligibility.periodKey());
        record.setDrawnAt(now);
        record.setFulfillmentStatus("PENDING");

        try {
            LotteryDrawRecordEntity saved = lotteryDrawRecordRepository.saveAndFlush(record);
            return new LotteryDrawResult(
                eligibilityFor(currentUser, now),
                toPrizeItem(prize),
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
        return lotteryPrizeRepository.findAllByOrderBySortOrderAsc().stream()
            .map(this::toPrizeItem)
            .toList();
    }

    public List<LotteryRecordItem> adminRecords() {
        currentUserService.requireAdmin(currentUserService.getCurrentUser());
        return lotteryDrawRecordRepository.findAllByOrderByDrawnAtDesc().stream()
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
        UserEntity targetUser = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        LotteryEligibilityResetEntity reset = new LotteryEligibilityResetEntity();
        reset.setId(UUID.randomUUID().toString());
        reset.setUser(targetUser);
        reset.setAdminUser(currentUser);
        reset.setReason(reason == null ? "" : reason.trim());
        reset.setCreatedAt(LocalDateTime.now());
        lotteryEligibilityResetRepository.save(reset);
        return true;
    }

    private LotteryEligibility eligibilityFor(UserEntity user, LocalDateTime now) {
        String vipLevel = vipService.levelForUser(user.getId());
        Period period = periodFor(vipLevel, now);
        Period effectivePeriod = resetPeriodIfAvailable(user, period);
        long drawCount = lotteryDrawRecordRepository.countByUser_IdAndPeriodTypeAndPeriodKey(
            user.getId(),
            effectivePeriod.type(),
            effectivePeriod.key()
        );
        boolean eligible = drawCount == 0;
        String nextAvailableAt = eligible ? "" : nextAvailableAt(period, now);
        String message = eligible ? "Lottery chance available." : "Next chance available at " + nextAvailableAt;
        return new LotteryEligibility(
            vipLevel,
            eligible,
            effectivePeriod.type(),
            effectivePeriod.key(),
            drawCount,
            nextAvailableAt,
            message
        );
    }

    private Period resetPeriodIfAvailable(UserEntity user, Period basePeriod) {
        LotteryDrawRecordEntity latestDraw = lotteryDrawRecordRepository.findFirstByUser_IdOrderByDrawnAtDesc(user.getId()).orElse(null);
        LotteryEligibilityResetEntity latestReset = lotteryEligibilityResetRepository.findFirstByUser_IdOrderByCreatedAtDesc(user.getId()).orElse(null);
        if (latestDraw == null || latestReset == null || !latestReset.getCreatedAt().isAfter(latestDraw.getDrawnAt())) {
            return basePeriod;
        }
        return new Period(basePeriod.type(), "RESET-" + latestReset.getId().replace("-", "").substring(0, 16));
    }

    private Period periodFor(String vipLevel, LocalDateTime now) {
        String normalized = vipLevel == null ? "VIP1" : vipLevel.toUpperCase(Locale.ROOT);
        if ("VIP2".equals(normalized)) {
            WeekFields weekFields = WeekFields.ISO;
            LocalDate date = now.toLocalDate();
            int week = date.get(weekFields.weekOfWeekBasedYear());
            int year = date.get(weekFields.weekBasedYear());
            return new Period("WEEK", "%04d-W%02d".formatted(year, week));
        }
        if ("VIP3".equals(normalized) || "VIP4".equals(normalized)) {
            return new Period("DAY", DAY_FORMATTER.format(now.toLocalDate()));
        }
        return new Period("ONCE", "WELCOME");
    }

    private String nextAvailableAt(Period basePeriod, LocalDateTime now) {
        if ("WEEK".equals(basePeriod.type())) {
            LocalDate nextMonday = now.toLocalDate().with(DayOfWeek.MONDAY).plusWeeks(1);
            return TIME_FORMATTER.format(nextMonday.atStartOfDay());
        }
        if ("DAY".equals(basePeriod.type())) {
            return TIME_FORMATTER.format(now.toLocalDate().plusDays(1).atStartOfDay());
        }
        return "After VIP upgrade or admin reset";
    }

    private LotteryPrizeEntity choosePrize(String requestedPrizeName) {
        List<LotteryPrizeEntity> prizes = lotteryPrizeRepository.findByEnabledTrueOrderBySortOrderAsc().stream()
            .filter(prize -> DRAWABLE_PRIZE_NAMES.contains(prize.getName()))
            .toList();
        if (prizes.isEmpty()) {
            throw new IllegalArgumentException("No drawable lottery prize is enabled");
        }
        if (StringUtils.hasText(requestedPrizeName)) {
            String normalized = requestedPrizeName.trim();
            if (!DRAWABLE_PRIZE_NAMES.contains(normalized)) {
                throw new IllegalArgumentException("Unsupported lottery prize");
            }
            return prizes.stream()
                .filter(prize -> normalized.equals(prize.getName()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Requested lottery prize is not enabled"));
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

    private LotteryPrizeItem toPrizeItem(LotteryPrizeEntity prize) {
        return new LotteryPrizeItem(
            prize.getId(),
            prize.getName(),
            prize.getPrizeType().toLowerCase(Locale.ROOT),
            prize.getWeightValue(),
            value(prize.getImageUrl()),
            prize.isEnabled(),
            prize.getSortOrder()
        );
    }

    private LotteryRecordItem toRecordItem(LotteryDrawRecordEntity record) {
        return new LotteryRecordItem(
            record.getId(),
            record.getUser().getUsername(),
            record.getVipLevel(),
            record.getPrize().getName(),
            record.getPrize().getPrizeType().toLowerCase(Locale.ROOT),
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
            new LotteryWinnerItem("+234 *** *** 4551", "₦5000", ""),
            new LotteryWinnerItem("+233 *** *** 7905", "₦3000", ""),
            new LotteryWinnerItem("+234 *** *** 2866", "₦1000", ""),
            new LotteryWinnerItem("+91 *** *** 1024", "iPad", "")
        );
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private record Period(String type, String key) {
    }
}
