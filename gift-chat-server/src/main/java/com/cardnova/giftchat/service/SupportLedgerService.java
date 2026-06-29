package com.cardnova.giftchat.service;

import com.cardnova.giftchat.entity.SupportConversationEntity;
import com.cardnova.giftchat.entity.TradeOrderEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.entity.WithdrawalRequestEntity;
import com.cardnova.giftchat.model.BalanceSummary;
import com.cardnova.giftchat.model.SupportLedgerCustomer;
import com.cardnova.giftchat.model.SupportLedgerReport;
import com.cardnova.giftchat.repository.SupportConversationRepository;
import com.cardnova.giftchat.repository.TradeOrderRepository;
import com.cardnova.giftchat.repository.WithdrawalRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class SupportLedgerService {

    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Set<String> PENDING_STATUSES = Set.of("PENDING", "PROCESSING");

    private final CurrentUserService currentUserService;
    private final SupportConversationRepository supportConversationRepository;
    private final TradeOrderRepository tradeOrderRepository;
    private final WithdrawalRequestRepository withdrawalRequestRepository;

    public SupportLedgerService(
        CurrentUserService currentUserService,
        SupportConversationRepository supportConversationRepository,
        TradeOrderRepository tradeOrderRepository,
        WithdrawalRequestRepository withdrawalRequestRepository
    ) {
        this.currentUserService = currentUserService;
        this.supportConversationRepository = supportConversationRepository;
        this.tradeOrderRepository = tradeOrderRepository;
        this.withdrawalRequestRepository = withdrawalRequestRepository;
    }

    public SupportLedgerReport report() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        currentUserService.requireAgentOrAdmin(currentUser);

        List<SupportConversationEntity> conversations = conversationsFor(currentUser).stream()
            .filter(conversation -> "ACTIVE".equalsIgnoreCase(conversation.getCustomerUser().getStatusCode()))
            .toList();
        List<SupportLedgerCustomer> customers = conversations.stream()
            .map(this::toCustomer)
            .sorted(Comparator.comparing((SupportLedgerCustomer customer) -> amountFromText(customer.pendingTotal())).reversed())
            .toList();

        BigDecimal available = customers.stream()
            .map(customer -> amountFromText(customer.availableTotal()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pending = customers.stream()
            .map(customer -> amountFromText(customer.pendingTotal()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal withdrawn = customers.stream()
            .map(customer -> amountFromText(customer.withdrawnTotal()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        String scope = "ADMIN".equalsIgnoreCase(currentUser.getRoleCode()) ? "all" : "own";
        return new SupportLedgerReport(
            new BalanceSummary(scope, money(available), money(pending), money(withdrawn), customers.size()),
            customers
        );
    }

    private List<SupportConversationEntity> conversationsFor(UserEntity currentUser) {
        if ("ADMIN".equalsIgnoreCase(currentUser.getRoleCode())) {
            return supportConversationRepository.findAllByOrderByUpdatedAtDesc();
        }
        return supportConversationRepository.findByAssignedAgent_IdOrderByUpdatedAtDesc(currentUser.getId());
    }

    private SupportLedgerCustomer toCustomer(SupportConversationEntity conversation) {
        UserEntity customer = conversation.getCustomerUser();
        List<TradeOrderEntity> orders = tradeOrderRepository.findByOwnerUser_IdOrderByUpdatedAtDesc(customer.getId());
        List<WithdrawalRequestEntity> withdrawals = withdrawalRequestRepository.findByOwnerUser_IdOrderByUpdatedAtDesc(customer.getId());

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

        int pendingOrderCount = (int) orders.stream()
            .filter(order -> PENDING_STATUSES.contains(order.getStatusCode().toUpperCase()))
            .count();

        return new SupportLedgerCustomer(
            conversation.getId(),
            customer.getUsername(),
            conversation.getAgentNote() == null || conversation.getAgentNote().isBlank()
                ? customer.getUsername()
                : conversation.getAgentNote(),
            conversation.getAssignedAgent() == null ? "" : conversation.getAssignedAgent().getUsername(),
            money(completed.subtract(withdrawn).max(BigDecimal.ZERO)),
            money(pending),
            money(withdrawn),
            orders.size(),
            pendingOrderCount,
            withdrawals.size(),
            TIME_FORMATTER.format(conversation.getUpdatedAt())
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

    private String money(BigDecimal value) {
        return MONEY_FORMAT.format(value);
    }
}
