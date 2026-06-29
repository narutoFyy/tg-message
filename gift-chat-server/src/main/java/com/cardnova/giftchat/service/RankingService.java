package com.cardnova.giftchat.service;

import com.cardnova.giftchat.entity.TradeOrderEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.model.RankingBoard;
import com.cardnova.giftchat.model.RankingEntry;
import com.cardnova.giftchat.repository.TradeOrderRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class RankingService {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("MMM, yyyy");

    private final TradeOrderRepository tradeOrderRepository;
    private final CurrentUserService currentUserService;

    public RankingService(TradeOrderRepository tradeOrderRepository, CurrentUserService currentUserService) {
        this.tradeOrderRepository = tradeOrderRepository;
        this.currentUserService = currentUserService;
    }

    public RankingBoard board(String mode, String month) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        YearMonth targetMonth = parseMonth(month);
        String normalizedMode = mode == null ? "sales" : mode.trim().toLowerCase();
        List<RankingEntry> entries = normalizedMode.equals("invitation")
            ? invitationEntries(currentUser, targetMonth)
            : salesEntries(currentUser, targetMonth);
        RankingEntry current = entries.stream()
            .filter(RankingEntry::currentUser)
            .findFirst()
            .orElse(new RankingEntry(500, currentUser.getUsername(), currentUser.getUsername(), "", "0", "₦0", true));
        return new RankingBoard(normalizedMode, MONTH_FORMATTER.format(targetMonth.atDay(1)), entries, current);
    }

    private List<RankingEntry> salesEntries(UserEntity currentUser, YearMonth month) {
        Map<String, Double> totals = new LinkedHashMap<>();
        tradeOrderRepository.findAllByOrderByUpdatedAtDesc().stream()
            .filter(order -> "COMPLETED".equalsIgnoreCase(order.getStatusCode()))
            .filter(order -> YearMonth.from(order.getUpdatedAt()).equals(month))
            .forEach(order -> totals.merge(order.getOwnerUser().getUsername(), numericAmount(order.getPayoutAmount()), Double::sum));

        // Seed a few presentation rows so the board has the same shape during early demos.
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
                "",
                formatNaira(row.getValue()),
                rewardFor(index + 1),
                row.getKey().equals(currentUser.getUsername())
            ));
        }
        return entries;
    }

    private List<RankingEntry> invitationEntries(UserEntity currentUser, YearMonth month) {
        List<RankingEntry> rows = new ArrayList<>();
        rows.add(new RankingEntry(1, "invite_lead_1", "Jay", "", "128 invites", "₦1.00m", false));
        rows.add(new RankingEntry(2, "invite_lead_2", "Mar**uce", "", "96 invites", "₦500.00k", false));
        rows.add(new RankingEntry(3, "invite_lead_3", "Sol**joh", "", "73 invites", "₦400.00k", false));
        rows.add(new RankingEntry(500, currentUser.getUsername(), currentUser.getUsername(), "", "0 invites", "₦0", true));
        return rows;
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
            return "₦" + String.format("%.2fm", value / 1_000_000);
        }
        if (value >= 1_000) {
            return "₦" + String.format("%.2fk", value / 1_000);
        }
        return "₦" + String.format("%.0f", value);
    }

    private String rewardFor(int rank) {
        if (rank == 1) return "₦1.00m";
        if (rank == 2) return "₦500.00k";
        if (rank == 3) return "₦400.00k";
        if (rank <= 10) return "₦300000";
        return "₦0";
    }

    private String maskName(String username) {
        if (username.length() <= 4 || username.contains("**")) {
            return username;
        }
        return username.substring(0, Math.min(3, username.length())) + "**" + username.substring(username.length() - 2);
    }
}
