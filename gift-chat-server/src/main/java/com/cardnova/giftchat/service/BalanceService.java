package com.cardnova.giftchat.service;

import com.cardnova.giftchat.entity.TradeOrderEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.entity.WithdrawalRequestEntity;
import com.cardnova.giftchat.model.BalanceSummary;
import com.cardnova.giftchat.model.CustomerBalanceSummary;
import com.cardnova.giftchat.repository.SupportConversationRepository;
import com.cardnova.giftchat.repository.TradeOrderRepository;
import com.cardnova.giftchat.repository.UserRepository;
import com.cardnova.giftchat.repository.WithdrawalRequestRepository;
import com.cardnova.giftchat.repository.WalletOperationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class BalanceService {

    private static final ThreadLocal<DecimalFormat> MONEY_FORMAT = ThreadLocal.withInitial(
        () -> new DecimalFormat("#,##0.00")
    );
    private static final Set<String> PENDING_STATUSES = Set.of("PENDING", "PROCESSING");
    private static final String WALLET_SOURCE = "WALLET";

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final SupportConversationRepository supportConversationRepository;
    private final TradeOrderRepository tradeOrderRepository;
    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final ReferralRewardService referralRewardService;
    private final RegistrationBonusService registrationBonusService;
    private final VipBenefitService vipBenefitService;
    private final WalletOperationRepository walletOperationRepository;

    public BalanceService(
        CurrentUserService currentUserService,
        UserRepository userRepository,
        SupportConversationRepository supportConversationRepository,
        TradeOrderRepository tradeOrderRepository,
        WithdrawalRequestRepository withdrawalRequestRepository,
        ReferralRewardService referralRewardService,
        RegistrationBonusService registrationBonusService,
        VipBenefitService vipBenefitService,
        WalletOperationRepository walletOperationRepository
    ) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.supportConversationRepository = supportConversationRepository;
        this.tradeOrderRepository = tradeOrderRepository;
        this.withdrawalRequestRepository = withdrawalRequestRepository;
        this.referralRewardService = referralRewardService;
        this.registrationBonusService = registrationBonusService;
        this.vipBenefitService = vipBenefitService;
        this.walletOperationRepository = walletOperationRepository;
    }

    public BalanceSummary summary() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        List<UserEntity> users;
        String scope;

        if ("ADMIN".equalsIgnoreCase(currentUser.getRoleCode())) {
            users = userRepository.findByRoleCodeAndStatusCodeOrderByCreatedAtAsc("USER", "ACTIVE");
            scope = "all";
        } else if ("AGENT".equalsIgnoreCase(currentUser.getRoleCode())) {
            users = supportConversationRepository.findByAssignedAgent_IdOrderByUpdatedAtDesc(currentUser.getId()).stream()
                .map(conversation -> conversation.getCustomerUser())
                .filter(user -> "ACTIVE".equalsIgnoreCase(user.getStatusCode()))
                .distinct()
                .toList();
            scope = "own";
        } else {
            users = List.of(currentUser);
            scope = "self";
        }

        List<String> userIds = users.stream().map(UserEntity::getId).toList();
        String currencyCode = commonCurrency(users);
        if (userIds.isEmpty()) {
            return new BalanceSummary(
                scope,
                currencyCode,
                money(BigDecimal.ZERO),
                money(BigDecimal.ZERO),
                money(BigDecimal.ZERO),
                money(BigDecimal.ZERO),
                money(BigDecimal.ZERO),
                0
            );
        }

        WalletAmounts amounts = walletAmounts(userIds);
        return new BalanceSummary(
            scope,
            currencyCode,
            money(amounts.available()),
            money(amounts.locked()),
            money(amounts.pendingSettlement()),
            money(amounts.pendingWithdrawal()),
            money(amounts.withdrawn()),
            users.size()
        );
    }

    public BigDecimal availableBalanceForUser(UserEntity user) {
        return walletAmounts(List.of(user.getId())).available();
    }

    public CustomerBalanceSummary customerSummary(UserEntity user) {
        WalletAmounts amounts = walletAmounts(List.of(user.getId()));
        return new CustomerBalanceSummary(
            money(amounts.available()),
            money(amounts.locked()),
            money(amounts.pendingSettlement()),
            money(amounts.pendingWithdrawal()),
            money(amounts.withdrawn())
        );
    }

    private WalletAmounts walletAmounts(List<String> userIds) {

        List<TradeOrderEntity> orders = tradeOrderRepository.findByOwnerUser_IdIn(userIds);
        List<WithdrawalRequestEntity> withdrawals = withdrawalRequestRepository.findByOwnerUser_IdIn(userIds);

        BigDecimal completed = orders.stream()
            .filter(order -> "COMPLETED".equalsIgnoreCase(order.getStatusCode()))
            .map(this::orderAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pending = orders.stream()
            .filter(order -> PENDING_STATUSES.contains(order.getStatusCode().toUpperCase()))
            .map(this::orderAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal withdrawn = walletWithdrawals(withdrawals)
            .filter(withdrawal -> "COMPLETED".equalsIgnoreCase(withdrawal.getStatusCode()))
            .map(withdrawal -> amountFromText(withdrawal.getAmount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pendingWithdrawal = walletWithdrawals(withdrawals)
            .filter(withdrawal -> "PENDING".equalsIgnoreCase(withdrawal.getStatusCode()))
            .map(withdrawal -> amountFromText(withdrawal.getAmount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal rewards = referralRewardService.availableRewardsForUsers(userIds);
        BigDecimal registrationBonuses = registrationBonusService.availableBonusesForUsers(userIds);
        BigDecimal lockedBonuses = registrationBonusService.lockedBonusesForUsers(userIds);
        BigDecimal vipBenefits = vipBenefitService.availableCreditsForUsers(userIds);
        BigDecimal manualAdjustments = walletOperationRepository.sumAmountDeltaByUserIds(userIds);

        BigDecimal available = completed
            .add(rewards)
            .add(registrationBonuses)
            .add(vipBenefits)
            .add(manualAdjustments == null ? BigDecimal.ZERO : manualAdjustments)
            .subtract(withdrawn)
            .subtract(pendingWithdrawal)
            .max(BigDecimal.ZERO);
        return new WalletAmounts(available, lockedBonuses, pending, pendingWithdrawal, withdrawn);
    }

    private java.util.stream.Stream<WithdrawalRequestEntity> walletWithdrawals(List<WithdrawalRequestEntity> withdrawals) {
        return withdrawals.stream().filter(withdrawal -> WALLET_SOURCE.equalsIgnoreCase(withdrawal.getSourceType()));
    }

    private BigDecimal orderAmount(TradeOrderEntity order) {
        return order.getLocalAmount() == null ? amountFromText(order.getPayoutAmount()) : order.getLocalAmount();
    }

    private String commonCurrency(List<UserEntity> users) {
        List<String> currencies = users.stream()
            .map(UserEntity::getCurrencyCode)
            .filter(currency -> currency != null && !currency.isBlank())
            .distinct()
            .toList();
        return currencies.size() == 1 ? currencies.get(0) : "";
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

    private String money(BigDecimal value) {
        return MONEY_FORMAT.get().format(value);
    }

    private record WalletAmounts(
        BigDecimal available,
        BigDecimal locked,
        BigDecimal pendingSettlement,
        BigDecimal pendingWithdrawal,
        BigDecimal withdrawn
    ) {
    }
}
