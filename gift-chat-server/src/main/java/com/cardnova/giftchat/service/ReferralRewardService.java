package com.cardnova.giftchat.service;

import com.cardnova.giftchat.dto.UpdateReferralRewardConfigRequest;
import com.cardnova.giftchat.entity.ReferralRewardConfigEntity;
import com.cardnova.giftchat.entity.ReferralRewardEntity;
import com.cardnova.giftchat.entity.TradeOrderEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.model.ReferralRewardConfigItem;
import com.cardnova.giftchat.model.ReferralRewardItem;
import com.cardnova.giftchat.repository.ReferralRewardConfigRepository;
import com.cardnova.giftchat.repository.ReferralRewardRepository;
import com.cardnova.giftchat.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ReferralRewardService {

    private static final String CONFIG_ID = "default";
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");

    private final ReferralRewardConfigRepository configRepository;
    private final ReferralRewardRepository rewardRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;
    private final InviteCodeService inviteCodeService;

    public ReferralRewardService(
        ReferralRewardConfigRepository configRepository,
        ReferralRewardRepository rewardRepository,
        UserRepository userRepository,
        CurrentUserService currentUserService,
        InviteCodeService inviteCodeService
    ) {
        this.configRepository = configRepository;
        this.rewardRepository = rewardRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
        this.inviteCodeService = inviteCodeService;
    }

    public ReferralRewardConfigItem config() {
        currentUserService.requireAdmin(currentUserService.getCurrentUser());
        return toConfigItem(getOrCreateConfig());
    }

    public List<ReferralRewardItem> rewards() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        currentUserService.requireAdmin(currentUser);
        return rewardRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toRewardItem).toList();
    }

    @Transactional
    public ReferralRewardConfigItem updateConfig(UpdateReferralRewardConfigRequest request) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        currentUserService.requireAdmin(currentUser);

        ReferralRewardConfigEntity config = getOrCreateConfig();
        config.setRegistrationCashbackEnabled(request.registrationCashbackEnabled());
        config.setRegistrationCashbackAmount(normalizeMoney(request.registrationCashbackAmount()));
        config.setTradeRebateEnabled(request.tradeRebateEnabled());
        config.setTradeRebatePercent(normalizePercent(request.tradeRebatePercent()));
        config.setUpdatedAt(LocalDateTime.now());
        config.setUpdatedBy(currentUser);
        return toConfigItem(configRepository.save(config));
    }

    public String generateInviteCode(String username) {
        return inviteCodeService.generatePersonalCode(username);
    }

    public UserEntity resolveReferrer(String inviteCode) {
        var resolved = inviteCodeService.resolveForRegistration(inviteCode);
        return resolved != null && InviteCodeService.PERSONAL.equals(resolved.getCodeType())
            ? resolved.getOwnerUser()
            : null;
    }

    @Transactional
    public void rewardRegistration(UserEntity referredUser) {
        if (referredUser == null || !StringUtils.hasText(referredUser.getId())) {
            return;
        }
        if (rewardRepository.existsByRewardTypeAndSourceKey("REGISTRATION", referredUser.getId())) {
            return;
        }

        UserEntity referrer = referrerFor(referredUser);
        if (referrer == null) {
            return;
        }

        ReferralRewardConfigEntity config = getOrCreateConfig();
        if (!config.isRegistrationCashbackEnabled() || config.getRegistrationCashbackAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        ReferralRewardEntity reward = new ReferralRewardEntity();
        reward.setId(UUID.randomUUID().toString());
        reward.setReferrerUser(referrer);
        reward.setReferredUser(referredUser);
        reward.setRewardType("REGISTRATION");
        reward.setSourceKey(referredUser.getId());
        reward.setAmount(normalizeMoney(config.getRegistrationCashbackAmount()));
        reward.setRatePercent(null);
        reward.setStatusCode("AVAILABLE");
        reward.setCreatedAt(LocalDateTime.now());
        reward.setUpdatedAt(LocalDateTime.now());
        rewardRepository.save(reward);
    }

    @Transactional
    public void rewardCompletedTrade(TradeOrderEntity order) {
        if (order == null || order.getOwnerUser() == null) {
            return;
        }
        if (!StringUtils.hasText(order.getId())) {
            return;
        }
        if (rewardRepository.existsByRewardTypeAndSourceKey("TRADE_REBATE", order.getId())) {
            return;
        }

        UserEntity referredUser = order.getOwnerUser();
        UserEntity referrer = referrerFor(referredUser);
        if (referrer == null) {
            return;
        }

        ReferralRewardConfigEntity config = getOrCreateConfig();
        if (!config.isTradeRebateEnabled() || config.getTradeRebatePercent().compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        BigDecimal baseAmount = amountFromText(order.getPayoutAmount());
        BigDecimal rewardAmount = baseAmount
            .multiply(config.getTradeRebatePercent())
            .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        if (rewardAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }

        ReferralRewardEntity reward = new ReferralRewardEntity();
        reward.setId(UUID.randomUUID().toString());
        reward.setReferrerUser(referrer);
        reward.setReferredUser(referredUser);
        reward.setTradeOrder(order);
        reward.setRewardType("TRADE_REBATE");
        reward.setSourceKey(order.getId());
        reward.setAmount(rewardAmount);
        reward.setRatePercent(normalizePercent(config.getTradeRebatePercent()));
        reward.setStatusCode("AVAILABLE");
        reward.setCreatedAt(LocalDateTime.now());
        reward.setUpdatedAt(LocalDateTime.now());
        rewardRepository.save(reward);
    }

    public BigDecimal availableRewardsForUsers(List<String> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return rewardRepository.findByReferrerUser_IdIn(userIds).stream()
            .filter(reward -> "AVAILABLE".equalsIgnoreCase(reward.getStatusCode()))
            .map(ReferralRewardEntity::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long invitationCount(String userId) {
        return userRepository.findByReferredByUserId(userId).size();
    }

    public BigDecimal totalRewardAmount(String userId) {
        return rewardRepository.findByReferrerUser_IdOrderByCreatedAtDesc(userId).stream()
            .filter(reward -> "AVAILABLE".equalsIgnoreCase(reward.getStatusCode()))
            .map(ReferralRewardEntity::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private UserEntity referrerFor(UserEntity referredUser) {
        if (referredUser == null || !StringUtils.hasText(referredUser.getReferredByUserId())) {
            return null;
        }
        if (referredUser.getId().equals(referredUser.getReferredByUserId())) {
            return null;
        }
        return userRepository.findById(referredUser.getReferredByUserId()).orElse(null);
    }

    private ReferralRewardConfigEntity getOrCreateConfig() {
        return configRepository.findById(CONFIG_ID).orElseGet(() -> {
            ReferralRewardConfigEntity config = new ReferralRewardConfigEntity();
            config.setId(CONFIG_ID);
            config.setRegistrationCashbackEnabled(false);
            config.setRegistrationCashbackAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            config.setTradeRebateEnabled(true);
            config.setTradeRebatePercent(BigDecimal.valueOf(5).setScale(4, RoundingMode.HALF_UP));
            config.setUpdatedAt(LocalDateTime.now());
            return configRepository.save(config);
        });
    }

    private ReferralRewardConfigItem toConfigItem(ReferralRewardConfigEntity config) {
        return new ReferralRewardConfigItem(
            config.isRegistrationCashbackEnabled(),
            money(config.getRegistrationCashbackAmount()),
            config.isTradeRebateEnabled(),
            percent(config.getTradeRebatePercent()),
            TIME_FORMATTER.format(config.getUpdatedAt()),
            config.getUpdatedBy() == null ? "" : config.getUpdatedBy().getUsername()
        );
    }

    private ReferralRewardItem toRewardItem(ReferralRewardEntity reward) {
        return new ReferralRewardItem(
            reward.getId(),
            reward.getReferrerUser().getUsername(),
            reward.getReferredUser().getUsername(),
            reward.getTradeOrder() == null ? "" : reward.getTradeOrder().getOrderNo(),
            reward.getRewardType().toLowerCase(Locale.ROOT),
            money(reward.getAmount()),
            reward.getRatePercent() == null ? "" : percent(reward.getRatePercent()),
            reward.getStatusCode().toLowerCase(Locale.ROOT),
            TIME_FORMATTER.format(reward.getCreatedAt())
        );
    }

    private BigDecimal amountFromText(String value) {
        if (value == null || value.isBlank()) {
            return BigDecimal.ZERO;
        }
        String normalized = value.replaceAll("[^0-9.]", "");
        if (normalized.isBlank()) {
            return BigDecimal.ZERO;
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException exception) {
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizePercent(BigDecimal value) {
        return value.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100)).setScale(4, RoundingMode.HALF_UP);
    }

    private String money(BigDecimal value) {
        return MONEY_FORMAT.format(value == null ? BigDecimal.ZERO : value);
    }

    private String percent(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).stripTrailingZeros().toPlainString();
    }

}
