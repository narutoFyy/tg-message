package com.cardnova.giftchat.service;

import com.cardnova.giftchat.entity.TradeOrderEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.entity.VipPointLedgerEntity;
import com.cardnova.giftchat.model.VipSummary;
import com.cardnova.giftchat.repository.VipPointLedgerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class VipService {

    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)");
    private static final Pattern QUANTITY_PATTERN = Pattern.compile("(?i)x\\s*(\\d+)");
    private static final DecimalFormat POINT_FORMAT = new DecimalFormat("#,##0.##");
    private static final List<VipTier> TIERS = List.of(
        new VipTier("VIP1", "Bronze", BigDecimal.ZERO),
        new VipTier("VIP2", "Silver", new BigDecimal("20")),
        new VipTier("VIP3", "Gold", new BigDecimal("100")),
        new VipTier("VIP4", "Diamond", new BigDecimal("500"))
    );

    private final VipPointLedgerRepository vipPointLedgerRepository;
    private final CurrentUserService currentUserService;

    public VipService(VipPointLedgerRepository vipPointLedgerRepository, CurrentUserService currentUserService) {
        this.vipPointLedgerRepository = vipPointLedgerRepository;
        this.currentUserService = currentUserService;
    }

    public VipSummary currentUserSummary() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        return summaryForUser(currentUser.getId());
    }

    public VipSummary summaryForUser(String userId) {
        return toSummary(pointsForUser(userId));
    }

    public BigDecimal pointsForUser(String userId) {
        if (!StringUtils.hasText(userId)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return vipPointLedgerRepository.sumPointsByUserId(userId).setScale(2, RoundingMode.HALF_UP);
    }

    public String levelForUser(String userId) {
        return tierFor(pointsForUser(userId)).level();
    }

    @Transactional
    public void awardCompletedOrderPoints(TradeOrderEntity order) {
        if (order == null || order.getOwnerUser() == null || !"COMPLETED".equalsIgnoreCase(order.getStatusCode())) {
            return;
        }
        String sourceKey = "TRADE_COMPLETED:" + order.getId();
        if (vipPointLedgerRepository.existsBySourceKey(sourceKey)) {
            return;
        }

        BigDecimal points = pointsFromOrder(order);
        if (points.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        VipPointLedgerEntity ledger = new VipPointLedgerEntity();
        ledger.setId(UUID.randomUUID().toString());
        ledger.setUser(order.getOwnerUser());
        ledger.setTradeOrder(order);
        ledger.setSourceKey(sourceKey);
        ledger.setPointsDelta(points.setScale(2, RoundingMode.HALF_UP));
        ledger.setReasonCode("TRADE_COMPLETED");
        ledger.setCreatedAt(LocalDateTime.now());
        vipPointLedgerRepository.save(ledger);
    }

    public VipSummary toSummary(BigDecimal points) {
        BigDecimal normalizedPoints = points == null ? BigDecimal.ZERO : points.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
        VipTier current = tierFor(normalizedPoints);
        VipTier next = nextTier(current);
        if (next == null) {
            return new VipSummary(
                current.level(),
                current.name(),
                formatPoints(normalizedPoints),
                "",
                "",
                "0",
                100,
                true
            );
        }

        BigDecimal currentThreshold = current.threshold();
        BigDecimal nextThreshold = next.threshold();
        BigDecimal span = nextThreshold.subtract(currentThreshold).max(BigDecimal.ONE);
        BigDecimal gainedInTier = normalizedPoints.subtract(currentThreshold).max(BigDecimal.ZERO);
        int progress = gainedInTier.multiply(new BigDecimal("100")).divide(span, 0, RoundingMode.DOWN).intValue();
        progress = Math.max(0, Math.min(99, progress));
        BigDecimal remaining = nextThreshold.subtract(normalizedPoints).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        return new VipSummary(
            current.level(),
            current.name(),
            formatPoints(normalizedPoints),
            next.level(),
            formatPoints(nextThreshold),
            formatPoints(remaining),
            progress,
            false
        );
    }

    private BigDecimal pointsFromOrder(TradeOrderEntity order) {
        BigDecimal faceValuePoints = amountFromFaceValue(order.getFaceValue());
        if (faceValuePoints.compareTo(BigDecimal.ZERO) > 0) {
            return faceValuePoints;
        }
        return firstAmount(order.getPayoutAmount());
    }

    private BigDecimal amountFromFaceValue(String value) {
        BigDecimal amount = firstAmount(value);
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        int quantity = 1;
        Matcher quantityMatcher = QUANTITY_PATTERN.matcher(value == null ? "" : value);
        if (quantityMatcher.find()) {
            try {
                quantity = Math.max(1, Integer.parseInt(quantityMatcher.group(1)));
            } catch (NumberFormatException ignored) {
                quantity = 1;
            }
        }
        return amount.multiply(new BigDecimal(quantity));
    }

    private BigDecimal firstAmount(String value) {
        if (!StringUtils.hasText(value)) {
            return BigDecimal.ZERO;
        }
        Matcher matcher = NUMBER_PATTERN.matcher(value.replace(",", ""));
        if (!matcher.find()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(matcher.group(1)).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException exception) {
            return BigDecimal.ZERO;
        }
    }

    private VipTier tierFor(BigDecimal points) {
        VipTier selected = TIERS.get(0);
        for (VipTier tier : TIERS) {
            if (points.compareTo(tier.threshold()) >= 0) {
                selected = tier;
            }
        }
        return selected;
    }

    private VipTier nextTier(VipTier current) {
        int index = TIERS.indexOf(current);
        if (index < 0 || index + 1 >= TIERS.size()) {
            return null;
        }
        return TIERS.get(index + 1);
    }

    private String formatPoints(BigDecimal value) {
        return POINT_FORMAT.format(value == null ? BigDecimal.ZERO : value);
    }

    private record VipTier(String level, String name, BigDecimal threshold) {
    }
}
