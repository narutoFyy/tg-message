package com.cardnova.giftchat.service;

import com.cardnova.giftchat.entity.LoanApplicationEntity;
import com.cardnova.giftchat.entity.LotteryFulfillmentOrderEntity;
import com.cardnova.giftchat.entity.SupportConversationEntity;
import com.cardnova.giftchat.entity.TradeOrderEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.entity.VideoSessionEntity;
import com.cardnova.giftchat.entity.WithdrawalRequestEntity;
import com.cardnova.giftchat.model.CustomerBalanceSummary;
import com.cardnova.giftchat.model.LoanApplicationItem;
import com.cardnova.giftchat.model.LotteryFulfillmentItem;
import com.cardnova.giftchat.model.SupportCustomerInfo;
import com.cardnova.giftchat.model.SupportCustomerProfile;
import com.cardnova.giftchat.model.TransactionItem;
import com.cardnova.giftchat.model.VideoSessionItem;
import com.cardnova.giftchat.model.WithdrawalItem;
import com.cardnova.giftchat.repository.LoanApplicationRepository;
import com.cardnova.giftchat.repository.LotteryFulfillmentOrderRepository;
import com.cardnova.giftchat.repository.TradeOrderRepository;
import com.cardnova.giftchat.repository.VideoSessionRepository;
import com.cardnova.giftchat.repository.WithdrawalRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class SupportCustomerProfileService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0.00");
    private static final Set<String> PENDING_STATUSES = Set.of("PENDING", "PROCESSING");

    private final PersistentSupportService persistentSupportService;
    private final TradeOrderRepository tradeOrderRepository;
    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final LotteryFulfillmentOrderRepository lotteryFulfillmentOrderRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final VideoSessionRepository videoSessionRepository;
    private final UserPresenceService userPresenceService;
    private final ReferralRewardService referralRewardService;
    private final RegistrationBonusService registrationBonusService;
    private final BankAccountRiskService bankAccountRiskService;
    private final CurrentUserService currentUserService;
    private final PhoneCountryCodeResolver phoneCountryCodeResolver;

    public SupportCustomerProfileService(
        PersistentSupportService persistentSupportService,
        TradeOrderRepository tradeOrderRepository,
        WithdrawalRequestRepository withdrawalRequestRepository,
        LotteryFulfillmentOrderRepository lotteryFulfillmentOrderRepository,
        LoanApplicationRepository loanApplicationRepository,
        VideoSessionRepository videoSessionRepository,
        UserPresenceService userPresenceService,
        ReferralRewardService referralRewardService,
        RegistrationBonusService registrationBonusService,
        BankAccountRiskService bankAccountRiskService,
        CurrentUserService currentUserService,
        PhoneCountryCodeResolver phoneCountryCodeResolver
    ) {
        this.persistentSupportService = persistentSupportService;
        this.tradeOrderRepository = tradeOrderRepository;
        this.withdrawalRequestRepository = withdrawalRequestRepository;
        this.lotteryFulfillmentOrderRepository = lotteryFulfillmentOrderRepository;
        this.loanApplicationRepository = loanApplicationRepository;
        this.videoSessionRepository = videoSessionRepository;
        this.userPresenceService = userPresenceService;
        this.referralRewardService = referralRewardService;
        this.registrationBonusService = registrationBonusService;
        this.bankAccountRiskService = bankAccountRiskService;
        this.currentUserService = currentUserService;
        this.phoneCountryCodeResolver = phoneCountryCodeResolver;
    }

    public SupportCustomerProfile getProfile(String conversationId) {
        SupportConversationEntity conversation = persistentSupportService.getAccessibleConversationForStaff(conversationId);
        UserEntity customer = conversation.getCustomerUser();

        List<TradeOrderEntity> orders = tradeOrderRepository.findByOwnerUser_IdOrderByUpdatedAtDesc(customer.getId());
        List<WithdrawalRequestEntity> withdrawals = withdrawalRequestRepository.findByOwnerUser_IdOrderByUpdatedAtDesc(customer.getId());
        List<LotteryFulfillmentOrderEntity> lotteryFulfillments = lotteryFulfillmentOrderRepository.findByOwnerUser_IdOrderByUpdatedAtDesc(customer.getId());
        List<LoanApplicationEntity> loans = loanApplicationRepository.findByOwnerUser_IdOrderByUpdatedAtDesc(customer.getId());
        List<VideoSessionEntity> videoSessions = videoSessionRepository.findByChannelTypeAndChannelIdOrderByCreatedAtDesc("SUPPORT", conversation.getId());

        return new SupportCustomerProfile(
            conversation.getId(),
            toCustomerInfo(conversation, customer),
            toBalance(customer, orders, withdrawals),
            orders.stream().map(order -> toTransactionItem(order, conversation)).toList(),
            withdrawals.stream().map(this::toWithdrawalItem).toList(),
            lotteryFulfillments.stream().map(this::toLotteryFulfillmentItem).toList(),
            loans.stream().map(this::toLoanItem).toList(),
            videoSessions.stream().map(this::toVideoSessionItem).toList(),
            registrationBonusService.recordForUser(customer.getId()),
            bankAccountRiskService.matchesForCustomer(customer, currentUserService.getCurrentUser())
        );
    }

    private SupportCustomerInfo toCustomerInfo(SupportConversationEntity conversation, UserEntity customer) {
        return new SupportCustomerInfo(
            customer.getId(),
            customer.getUsername(),
            value(customer.getAvatarUrl()),
            value(customer.getEmail()),
            value(customer.getPhone()),
            phoneCountryCodeResolver.resolve(customer.getPhone(), registrationBonusService.configuredCountryCodes()),
            value(customer.getStatusCode()),
            value(conversation.getAgentNote()),
            userPresenceService.isOnline(customer.getId()),
            conversation.getAssignedAgent() == null ? "" : conversation.getAssignedAgent().getUsername(),
            TIME_FORMATTER.format(customer.getCreatedAt()),
            TIME_FORMATTER.format(customer.getUpdatedAt())
        );
    }

    private CustomerBalanceSummary toBalance(UserEntity customer, List<TradeOrderEntity> orders, List<WithdrawalRequestEntity> withdrawals) {
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
            .filter(withdrawal -> "WALLET".equalsIgnoreCase(withdrawal.getSourceType()))
            .map(withdrawal -> amountFromText(withdrawal.getAmount()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal rewards = referralRewardService.availableRewardsForUsers(List.of(customer.getId()));
        BigDecimal registrationBonuses = registrationBonusService.availableBonusesForUsers(List.of(customer.getId()));

        return new CustomerBalanceSummary(
            money(completed.add(rewards).add(registrationBonuses).subtract(withdrawn).max(BigDecimal.ZERO)),
            money(pending),
            money(withdrawn)
        );
    }

    private TransactionItem toTransactionItem(TradeOrderEntity order, SupportConversationEntity conversation) {
        UserEntity counterpart = order.getCounterpartyUser();
        String assignedAgent = conversation.getAssignedAgent() == null ? "" : conversation.getAssignedAgent().getUsername();
        return new TransactionItem(
            order.getId(),
            order.getOrderNo(),
            order.getCardName(),
            order.getFaceValue(),
            order.getPayoutAmount(),
            order.getStatusCode().toLowerCase(),
            counterpart == null ? assignedAgent : counterpart.getUsername(),
            counterpart == null ? assignedAgent : counterpart.getUsername(),
            order.getFriendship() == null ? "" : order.getFriendship().getId(),
            value(order.getNote()),
            value(order.getVoucherImageUrl()),
            value(order.getCancelReason()),
            value(order.getCancelNote()),
            order.getCanceledByUser() == null ? "" : order.getCanceledByUser().getUsername(),
            order.getCanceledAt() == null ? "" : TIME_FORMATTER.format(order.getCanceledAt()),
            TIME_FORMATTER.format(order.getCreatedAt()),
            TIME_FORMATTER.format(order.getUpdatedAt())
        );
    }

    private WithdrawalItem toWithdrawalItem(WithdrawalRequestEntity entity) {
        return new WithdrawalItem(
            entity.getId(),
            entity.getRequestNo(),
            entity.getSourceType().toLowerCase(),
            entity.getOwnerUser().getUsername(),
            entity.getLotteryDrawRecord() == null ? "" : entity.getLotteryDrawRecord().getId(),
            entity.getLotteryDrawRecord() == null ? "" : entity.getLotteryDrawRecord().getPrize().getName(),
            entity.getLotteryDrawRecord() == null ? "" : entity.getLotteryDrawRecord().getPrize().getPrizeType().toLowerCase(),
            entity.getAmount(),
            entity.getCountry(),
            entity.getAccountName(),
            entity.getBankName(),
            entity.getAccountNumber(),
            value(entity.getContact()),
            value(entity.getNote()),
            entity.getStatusCode().toLowerCase(),
            entity.getAssignedAgent() == null ? "" : entity.getAssignedAgent().getUsername(),
            TIME_FORMATTER.format(entity.getCreatedAt()),
            TIME_FORMATTER.format(entity.getUpdatedAt())
        );
    }

    private LotteryFulfillmentItem toLotteryFulfillmentItem(LotteryFulfillmentOrderEntity entity) {
        return new LotteryFulfillmentItem(
            entity.getId(),
            entity.getOrderNo(),
            entity.getOwnerUser().getUsername(),
            entity.getLotteryDrawRecord().getId(),
            entity.getLotteryDrawRecord().getPrize().getName(),
            entity.getLotteryDrawRecord().getPrize().getPrizeType().toLowerCase(Locale.ROOT),
            entity.getRecipientName(),
            entity.getPhone(),
            entity.getCountry(),
            entity.getAddressLine(),
            entity.getStatusCode().toLowerCase(Locale.ROOT),
            entity.getAssignedAgent() == null ? "" : entity.getAssignedAgent().getUsername(),
            TIME_FORMATTER.format(entity.getCreatedAt()),
            TIME_FORMATTER.format(entity.getUpdatedAt())
        );
    }

    private LoanApplicationItem toLoanItem(LoanApplicationEntity entity) {
        return new LoanApplicationItem(
            entity.getId(),
            entity.getApplicationNo(),
            entity.getOwnerUser().getUsername(),
            entity.getAmount(),
            entity.getCountry(),
            entity.getPurpose(),
            value(entity.getContact()),
            value(entity.getRepaymentPlan()),
            entity.getStatusCode().toLowerCase(),
            value(entity.getReviewNote()),
            entity.getAssignedAgent() == null ? "" : entity.getAssignedAgent().getUsername(),
            entity.getReviewedBy() == null ? "" : entity.getReviewedBy().getUsername(),
            TIME_FORMATTER.format(entity.getCreatedAt()),
            TIME_FORMATTER.format(entity.getUpdatedAt())
        );
    }

    private VideoSessionItem toVideoSessionItem(VideoSessionEntity entity) {
        return new VideoSessionItem(
            entity.getId(),
            entity.getRoomId(),
            entity.getChannelType().toLowerCase(),
            entity.getChannelId(),
            entity.getInitiatorUser().getUsername(),
            entity.getReceiverUser().getUsername(),
            entity.getVendorCode().toLowerCase(),
            entity.getStatusCode().toLowerCase(),
            entity.getStartedAt() == null ? "" : TIME_FORMATTER.format(entity.getStartedAt()),
            entity.getEndedAt() == null ? "" : TIME_FORMATTER.format(entity.getEndedAt()),
            TIME_FORMATTER.format(entity.getCreatedAt()),
            TIME_FORMATTER.format(entity.getUpdatedAt())
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

    private String value(String value) {
        return value == null ? "" : value;
    }
}
