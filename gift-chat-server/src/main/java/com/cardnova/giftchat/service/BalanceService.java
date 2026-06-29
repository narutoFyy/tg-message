package com.cardnova.giftchat.service;

import com.cardnova.giftchat.entity.TradeOrderEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.entity.WithdrawalRequestEntity;
import com.cardnova.giftchat.model.BalanceSummary;
import com.cardnova.giftchat.repository.SupportConversationRepository;
import com.cardnova.giftchat.repository.TradeOrderRepository;
import com.cardnova.giftchat.repository.UserRepository;
import com.cardnova.giftchat.repository.WithdrawalRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class BalanceService {

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");
    private static final Set<String> PENDING_STATUSES = Set.of("PENDING", "PROCESSING");

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final SupportConversationRepository supportConversationRepository;
    private final TradeOrderRepository tradeOrderRepository;
    private final WithdrawalRequestRepository withdrawalRequestRepository;

    public BalanceService(
        CurrentUserService currentUserService,
        UserRepository userRepository,
        SupportConversationRepository supportConversationRepository,
        TradeOrderRepository tradeOrderRepository,
        WithdrawalRequestRepository withdrawalRequestRepository
    ) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.supportConversationRepository = supportConversationRepository;
        this.tradeOrderRepository = tradeOrderRepository;
        this.withdrawalRequestRepository = withdrawalRequestRepository;
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
        if (userIds.isEmpty()) {
            return new BalanceSummary(scope, money(BigDecimal.ZERO), money(BigDecimal.ZERO), money(BigDecimal.ZERO), 0);
        }

        List<TradeOrderEntity> orders = tradeOrderRepository.findByOwnerUser_IdIn(userIds);
        List<WithdrawalRequestEntity> withdrawals = withdrawalRequestRepository.findByOwnerUser_IdIn(userIds);

        BigDecimal completed = orders.stream()
            .filter(order -> "COMPLETED".equalsIgnoreCase(order.getStatusCode()))
            .map(order -> amountFromText(order.getPayoutAmount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pending = orders.stream()
            .filter(order -> PENDING_STATUSES.contains(order.getStatusCode().toUpperCase()))
            .map(order -> amountFromText(order.getPayoutAmount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal withdrawn = withdrawals.stream()
            .filter(withdrawal -> "COMPLETED".equalsIgnoreCase(withdrawal.getStatusCode()))
            .map(withdrawal -> amountFromText(withdrawal.getAmount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal available = completed.subtract(withdrawn).max(BigDecimal.ZERO);
        return new BalanceSummary(scope, money(available), money(pending), money(withdrawn), users.size());
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
        return MONEY_FORMAT.format(value);
    }
}
