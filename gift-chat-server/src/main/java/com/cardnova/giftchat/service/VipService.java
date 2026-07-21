package com.cardnova.giftchat.service;

import com.cardnova.giftchat.entity.TradeOrderEntity;
import com.cardnova.giftchat.entity.VipPointLedgerEntity;
import com.cardnova.giftchat.model.VipSummary;
import com.cardnova.giftchat.repository.TradeOrderRepository;
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

@Service
@Transactional(readOnly = true)
public class VipService {

    private static final DecimalFormat USD_FORMAT = new DecimalFormat("#,##0.##");
    private static final List<VipTier> TIERS = List.of(
        new VipTier("VIP1", "Bronze", BigDecimal.ZERO),
        new VipTier("VIP2", "Silver", new BigDecimal("1000")),
        new VipTier("VIP3", "Gold", new BigDecimal("5000")),
        new VipTier("VIP4", "Platinum", new BigDecimal("10000")),
        new VipTier("VIP5", "Diamond", new BigDecimal("50000"))
    );

    private final VipPointLedgerRepository vipPointLedgerRepository;
    private final TradeOrderRepository tradeOrderRepository;
    private final CurrentUserService currentUserService;

    public VipService(
        VipPointLedgerRepository vipPointLedgerRepository,
        TradeOrderRepository tradeOrderRepository,
        CurrentUserService currentUserService
    ) {
        this.vipPointLedgerRepository = vipPointLedgerRepository;
        this.tradeOrderRepository = tradeOrderRepository;
        this.currentUserService = currentUserService;
    }

    public VipSummary currentUserSummary() {
        return summaryForUser(currentUserService.getCurrentUser().getId());
    }

    public VipSummary summaryForUser(String userId) {
        BigDecimal volume = tradeVolumeUsdForUser(userId);
        return toSummary(volume, hasCompletedTrade(userId));
    }

    public BigDecimal pointsForUser(String userId) {
        return tradeVolumeUsdForUser(userId);
    }

    public BigDecimal tradeVolumeUsdForUser(String userId) {
        if (!StringUtils.hasText(userId)) {
            return money(BigDecimal.ZERO);
        }
        return money(tradeOrderRepository.sumCompletedBaseAmountUsdByOwnerUserId(userId));
    }

    public boolean hasCompletedTrade(String userId) {
        return StringUtils.hasText(userId)
            && tradeOrderRepository.existsByOwnerUser_IdAndStatusCodeIgnoreCase(userId, "COMPLETED");
    }

    public String levelForUser(String userId) {
        return tierFor(tradeVolumeUsdForUser(userId), hasCompletedTrade(userId)).level();
    }

    public boolean isAtLeast(String userId, int requiredLevel) {
        String level = levelForUser(userId);
        return level.matches("VIP[0-5]") && Integer.parseInt(level.substring(3)) >= requiredLevel;
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
        BigDecimal amount = order.getBaseAmountUsd() == null ? BigDecimal.ZERO : order.getBaseAmountUsd();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        VipPointLedgerEntity ledger = new VipPointLedgerEntity();
        ledger.setId(UUID.randomUUID().toString());
        ledger.setUser(order.getOwnerUser());
        ledger.setTradeOrder(order);
        ledger.setSourceKey(sourceKey);
        ledger.setPointsDelta(money(amount));
        ledger.setReasonCode("TRADE_COMPLETED_USD");
        ledger.setCreatedAt(LocalDateTime.now());
        vipPointLedgerRepository.save(ledger);
    }

    private VipSummary toSummary(BigDecimal volume, boolean hasCompletedTrade) {
        BigDecimal normalized = money(volume == null ? BigDecimal.ZERO : volume.max(BigDecimal.ZERO));
        VipTier current = tierFor(normalized, hasCompletedTrade);
        if ("VIP0".equals(current.level())) {
            return new VipSummary("VIP0", "New", format(normalized), "VIP1", "First completed trade", "1 completed trade", 0, false);
        }
        VipTier next = nextTier(current);
        if (next == null) {
            return new VipSummary(current.level(), current.name(), format(normalized), "", "", "0", 100, true);
        }
        BigDecimal currentThreshold = current.threshold();
        BigDecimal nextThreshold = next.threshold();
        BigDecimal span = nextThreshold.subtract(currentThreshold).max(BigDecimal.ONE);
        BigDecimal gained = normalized.subtract(currentThreshold).max(BigDecimal.ZERO);
        int progress = gained.multiply(new BigDecimal("100")).divide(span, 0, RoundingMode.DOWN).intValue();
        BigDecimal remaining = nextThreshold.subtract(normalized).max(BigDecimal.ZERO);
        return new VipSummary(
            current.level(),
            current.name(),
            format(normalized),
            next.level(),
            format(nextThreshold),
            format(remaining),
            Math.max(0, Math.min(99, progress)),
            false
        );
    }

    private VipTier tierFor(BigDecimal volume, boolean hasCompletedTrade) {
        if (!hasCompletedTrade) {
            return new VipTier("VIP0", "New", BigDecimal.ZERO);
        }
        VipTier selected = TIERS.get(0);
        for (VipTier tier : TIERS) {
            if (volume.compareTo(tier.threshold()) >= 0) {
                selected = tier;
            }
        }
        return selected;
    }

    private VipTier nextTier(VipTier current) {
        int index = TIERS.indexOf(current);
        return index < 0 || index + 1 >= TIERS.size() ? null : TIERS.get(index + 1);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String format(BigDecimal value) {
        return USD_FORMAT.format(value == null ? BigDecimal.ZERO : value);
    }

    private record VipTier(String level, String name, BigDecimal threshold) {
    }
}
