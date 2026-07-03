package com.cardnova.giftchat.service;

import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.model.RankingBoard;
import com.cardnova.giftchat.model.RankingEntry;
import com.cardnova.giftchat.repository.TradeOrderRepository;
import com.cardnova.giftchat.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class RankingService {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMM, yyyy");

    private final TradeOrderRepository tradeOrderRepository;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final ReferralRewardService referralRewardService;

    public RankingService(
        TradeOrderRepository tradeOrderRepository,
        CurrentUserService currentUserService,
        UserRepository userRepository,
        ReferralRewardService referralRewardService
    ) {
        this.tradeOrderRepository = tradeOrderRepository;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.referralRewardService = referralRewardService;
    }

    public RankingBoard board(String mode, String month) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        YearMonth targetMonth = parseMonth(month);
        String normalizedMode = normalizeMode(mode);
        List<RankingEntry> entries = normalizedMode.equals("invitation")
            ? invitationEntries(currentUser)
            : salesEntries(currentUser, targetMonth);
        RankingEntry current = entries.stream()
            .filter(RankingEntry::currentUser)
            .findFirst()
            .orElse(currentUserEntry(currentUser, normalizedMode));
        return new RankingBoard(normalizedMode, MONTH_FORMATTER.format(targetMonth.atDay(1)), entries, current);
    }

    private List<RankingEntry> salesEntries(UserEntity currentUser, YearMonth month) {
        Map<String, Double> totals = new LinkedHashMap<>();
        tradeOrderRepository.findAllByOrderByUpdatedAtDesc().stream()
            .filter(order -> "COMPLETED".equalsIgnoreCase(order.getStatusCode()))
            .filter(order -> YearMonth.from(order.getUpdatedAt()).equals(month))
            .forEach(order -> totals.merge(order.getOwnerUser().getUsername(), numericAmount(order.getPayoutAmount()), Double::sum));

        totals.putIfAbsent("Oni**ide", 22_680_000d);
        totals.putIfAbsent("Suc**ovo", 18_990_000d);
        totals.putIfAbsent("IYA**ode", 17_850_000d);
        totals.putIfAbsent("Ima**chy", 15_060_000d);
        totals.putIfAbsent(currentUser.getUsername(), totals.getOrDefault(currentUser.getUsername(), 0d));

        List<Map.Entry<String, Double>> sorted = totals.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder()))
            .toList();
        List<RankingEntry> entries = new ArrayList<>();
        for (int index = 0; index < sorted.size(); index++) {
            Map.Entry<String, Double> row = sorted.get(index);
            entries.add(new RankingEntry(
                index + 1,
                row.getKey(),
                maskName(row.getKey()),
                row.getKey().equals(currentUser.getUsername()) ? safeAvatar(currentUser) : "",
                formatNaira(row.getValue()),
                salesRewardFor(index + 1),
                row.getKey().equals(currentUser.getUsername())
            ));
        }
        return entries;
    }

    private List<RankingEntry> invitationEntries(UserEntity currentUser) {
        List<UserEntity> users = new ArrayList<>(userRepository.findByRoleCodeAndStatusCodeOrderByCreatedAtAsc("USER", "ACTIVE"));
        if (users.stream().noneMatch(user -> user.getId().equals(currentUser.getId()))) {
            users.add(currentUser);
        }

        List<UserInvitationScore> sorted = users.stream()
            .map(user -> new UserInvitationScore(
                user,
                referralRewardService.invitationCount(user.getId()),
                referralRewardService.totalRewardAmount(user.getId())
            ))
            .sorted(Comparator.comparingLong(UserInvitationScore::invitationCount).reversed()
                .thenComparing(score -> score.user().getCreatedAt()))
            .toList();

        List<RankingEntry> entries = new ArrayList<>();
        for (int index = 0; index < sorted.size(); index++) {
            UserInvitationScore score = sorted.get(index);
            UserEntity user = score.user();
            boolean current = user.getId().equals(currentUser.getId());
            long count = score.invitationCount();
            entries.add(new RankingEntry(
                index + 1,
                user.getUsername(),
                current ? user.getUsername() : maskName(user.getUsername()),
                current ? safeAvatar(currentUser) : safeAvatar(user),
                count + (count == 1 ? " invite" : " invites"),
                formatMoney(score.rewardAmount()),
                current
            ));
        }
        return entries;
    }

    private RankingEntry currentUserEntry(UserEntity currentUser, String mode) {
        String score = mode.equals("invitation") ? "0 invites" : "NGN 0";
        return new RankingEntry(500, currentUser.getUsername(), currentUser.getUsername(), safeAvatar(currentUser), score, "NGN 0", true);
    }

    private YearMonth parseMonth(String value) {
        if (value == null || value.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(value.trim());
        } catch (RuntimeException ignored) {
            return YearMonth.now();
        }
    }

    private double numericAmount(String value) {
        String digits = value == null ? "" : value.replaceAll("[^0-9.]", "");
        if (digits.isBlank()) {
            return 0;
        }
        return Double.parseDouble(digits);
    }

    private String formatNaira(double value) {
        if (value >= 1_000_000) {
            return "NGN " + String.format("%.2fm", value / 1_000_000);
        }
        if (value >= 1_000) {
            return "NGN " + String.format("%.2fk", value / 1_000);
        }
        return "NGN " + String.format("%.0f", value);
    }

    private String formatMoney(BigDecimal value) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        return "NGN " + safeValue.stripTrailingZeros().toPlainString();
    }

    private String salesRewardFor(int rank) {
        if (rank == 1) return "NGN 1.00m";
        if (rank == 2) return "NGN 500.00k";
        if (rank == 3) return "NGN 400.00k";
        if (rank <= 10) return "NGN 100000";
        return "NGN 0";
    }

    private String normalizeMode(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase();
        return normalized.equals("invitation") ? "invitation" : "sales";
    }

    private String safeAvatar(UserEntity user) {
        return user.getAvatarUrl() == null ? "" : user.getAvatarUrl();
    }

    private String maskName(String username) {
        if (username.length() <= 4 || username.contains("**")) {
            return username;
        }
        return username.substring(0, Math.min(3, username.length())) + "**" + username.substring(username.length() - 2);
    }

    private record UserInvitationScore(UserEntity user, long invitationCount, BigDecimal rewardAmount) {
    }
}
