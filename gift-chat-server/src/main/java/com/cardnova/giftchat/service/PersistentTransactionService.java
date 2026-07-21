package com.cardnova.giftchat.service;

import com.cardnova.giftchat.api.ConflictException;
import com.cardnova.giftchat.dto.CreateTransactionRequest;
import com.cardnova.giftchat.dto.CreateSellOrderRequest;
import com.cardnova.giftchat.dto.CompleteTransactionRequest;
import com.cardnova.giftchat.entity.SupportConversationEntity;
import com.cardnova.giftchat.entity.FriendshipEntity;
import com.cardnova.giftchat.entity.TradeOrderEntity;
import com.cardnova.giftchat.entity.TradeOrderSettlementAuditEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.entity.GiftCardRateEntity;
import com.cardnova.giftchat.model.TransactionItem;
import com.cardnova.giftchat.model.CompletedTransactionFeedItem;
import com.cardnova.giftchat.repository.BlacklistEntryRepository;
import com.cardnova.giftchat.repository.FriendshipRepository;
import com.cardnova.giftchat.repository.SupportConversationRepository;
import com.cardnova.giftchat.repository.TradeOrderRepository;
import com.cardnova.giftchat.repository.TradeOrderSettlementAuditRepository;
import com.cardnova.giftchat.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PersistentTransactionService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Set<String> ALLOWED_STATUSES = Set.of("pending", "processing", "disputed");
    private static final Set<String> CANCELABLE_STATUSES = Set.of("PENDING", "PROCESSING");

    private final TradeOrderRepository tradeOrderRepository;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final BlacklistEntryRepository blacklistEntryRepository;
    private final PersistentSupportService persistentSupportService;
    private final NotificationService notificationService;
    private final SupportConversationRepository supportConversationRepository;
    private final ReferralRewardService referralRewardService;
    private final VipService vipService;
    private final UserHiddenRecordService userHiddenRecordService;
    private final PersistentRateService persistentRateService;
    private final CountryCodeService countryCodeService;
    private final TradeOrderNumberService tradeOrderNumberService;
    private final TradeOrderSettlementAuditRepository tradeOrderSettlementAuditRepository;

    public PersistentTransactionService(
        TradeOrderRepository tradeOrderRepository,
        CurrentUserService currentUserService,
        UserRepository userRepository,
        FriendshipRepository friendshipRepository,
        BlacklistEntryRepository blacklistEntryRepository,
        PersistentSupportService persistentSupportService,
        NotificationService notificationService,
        SupportConversationRepository supportConversationRepository,
        ReferralRewardService referralRewardService,
        VipService vipService,
        UserHiddenRecordService userHiddenRecordService,
        PersistentRateService persistentRateService,
        CountryCodeService countryCodeService,
        TradeOrderNumberService tradeOrderNumberService,
        TradeOrderSettlementAuditRepository tradeOrderSettlementAuditRepository
    ) {
        this.tradeOrderRepository = tradeOrderRepository;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.blacklistEntryRepository = blacklistEntryRepository;
        this.persistentSupportService = persistentSupportService;
        this.notificationService = notificationService;
        this.supportConversationRepository = supportConversationRepository;
        this.referralRewardService = referralRewardService;
        this.vipService = vipService;
        this.userHiddenRecordService = userHiddenRecordService;
        this.persistentRateService = persistentRateService;
        this.countryCodeService = countryCodeService;
        this.tradeOrderNumberService = tradeOrderNumberService;
        this.tradeOrderSettlementAuditRepository = tradeOrderSettlementAuditRepository;
    }

    public List<TransactionItem> getTransactions() {
        UserEntity currentUser = currentUserService.getCurrentUser();

        if (isAdmin(currentUser)) {
            return tradeOrderRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(order -> toTransactionItem(order, currentUser.getId()))
                .toList();
        }

        return tradeOrderRepository.findByOwnerUser_IdOrCounterpartyUser_IdOrderByUpdatedAtDesc(currentUser.getId(), currentUser.getId()).stream()
            .filter(order -> shouldShowToCurrentUser(order, currentUser))
            .map(order -> toTransactionItem(order, currentUser.getId()))
            .toList();
    }

    public TransactionItem getTransaction(String transactionId) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        TradeOrderEntity order = tradeOrderRepository.findById(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        if (!canAccess(order, currentUser)) {
            throw new IllegalArgumentException("Transaction not accessible");
        }
        if (!shouldShowToCurrentUser(order, currentUser)) {
            throw new IllegalArgumentException("Transaction not accessible");
        }
        return toTransactionItem(order, currentUser.getId());
    }

    @Transactional
    public TransactionItem createTransaction(CreateTransactionRequest request) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        UserEntity counterparty = userRepository.findByUsername(request.counterpartyUsername().trim())
            .orElseThrow(() -> new IllegalArgumentException("Counterparty not found"));

        if (currentUser.getId().equals(counterparty.getId())) {
            throw new IllegalArgumentException("Cannot create a trade with yourself");
        }
        if (blacklistEntryRepository.existsByOwnerUser_IdAndBlockedUser_Id(currentUser.getId(), counterparty.getId())
            || blacklistEntryRepository.existsByOwnerUser_IdAndBlockedUser_Id(counterparty.getId(), currentUser.getId())) {
            throw new IllegalArgumentException("Trade blocked by blacklist relationship");
        }

        FriendshipEntity friendship = resolveFriendship(currentUser.getId(), counterparty.getId(), request.friendshipId());
        if (friendship == null || !"ACCEPTED".equals(friendship.getStatusCode())) {
            throw new IllegalArgumentException("An accepted friendship is required before creating a trade");
        }

        TradeOrderEntity entity = new TradeOrderEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setOrderNo(tradeOrderNumberService.nextOrderNo());
        entity.setOwnerUser(currentUser);
        entity.setCounterpartyUser(counterparty);
        entity.setFriendship(friendship);
        entity.setCardName(request.cardName().trim());
        entity.setFaceValue(request.faceValue().trim());
        entity.setPayoutAmount(request.payoutAmount().trim());
        entity.setStatusCode("PENDING");
        entity.setNote(normalizeNullable(request.note()));
        entity.setVoucherImageUrl(normalizeNullable(request.voucherImageUrl()));
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        TradeOrderEntity saved = tradeOrderRepository.save(entity);
        return toTransactionItem(saved, currentUser.getId());
    }

    @Transactional
    public TransactionItem createSellOrder(CreateSellOrderRequest request) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        if (!"USER".equalsIgnoreCase(currentUser.getRoleCode())) {
            throw new IllegalArgumentException("Only users can create sell orders");
        }

        String clientRequestId = normalizeClientRequestId(request.clientRequestId());
        String requestHash = clientRequestId == null ? null : sellOrderRequestHash(request);
        if (clientRequestId != null) {
            currentUser = userRepository.findByIdForUpdate(currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
            TradeOrderEntity existingOrder = tradeOrderRepository
                .findByOwnerUser_IdAndClientRequestId(currentUser.getId(), clientRequestId)
                .orElse(null);
            if (existingOrder != null) {
                if (!requestHash.equals(existingOrder.getClientRequestHash())) {
                    throw new ConflictException("Idempotency key was already used for a different sell order");
                }
                return toTransactionItem(existingOrder, currentUser.getId());
            }
        }

        SupportConversationEntity conversation = persistentSupportService.ensureUserConversation(currentUser);
        UserEntity assignedAgent = conversation == null ? null : conversation.getAssignedAgent();
        if (assignedAgent == null) {
            throw new IllegalArgumentException("No active support agent available");
        }

        String faceCurrency = normalizeFaceCurrency(request.cardCountry());
        String faceValue = formatFaceValue(request.faceValue(), faceCurrency, request.quantity());
        PersistentRateService.RateQuote selectedQuote = persistentRateService.requireActiveQuote(request.cardName(), currentUser.getCountryCode(), faceCurrency);
        PersistentRateService.RateQuote usdQuote = "USD".equals(faceCurrency)
            ? selectedQuote
            : persistentRateService.requireActiveQuote(request.cardName(), currentUser.getCountryCode(), "USD");
        BigDecimal faceAmount = BigDecimal.valueOf(request.faceValue()).setScale(6, RoundingMode.HALF_UP);
        BigDecimal totalFaceAmount = faceAmount.multiply(BigDecimal.valueOf(request.quantity())).setScale(6, RoundingMode.HALF_UP);
        BigDecimal faceToUsdRate = selectedQuote.localPayoutPerUnit()
            .divide(usdQuote.localPayoutPerUnit(), 6, RoundingMode.HALF_UP);
        BigDecimal baseAmountUsd = totalFaceAmount.multiply(faceToUsdRate).setScale(6, RoundingMode.HALF_UP);
        BigDecimal localAmount = totalFaceAmount.multiply(selectedQuote.localPayoutPerUnit()).setScale(2, RoundingMode.HALF_UP);
        var country = countryCodeService.requireCountry(currentUser.getCountryCode());
        String note = sellOrderNote(request, selectedQuote.localPayoutPerUnit());

        TradeOrderEntity entity = new TradeOrderEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setOrderNo(tradeOrderNumberService.nextOrderNo());
        entity.setOwnerUser(currentUser);
        entity.setCounterpartyUser(assignedAgent);
        entity.setFriendship(null);
        entity.setCardName(request.cardName().trim());
        entity.setFaceValue(faceValue);
        entity.setFaceCurrencyCode(faceCurrency);
        entity.setFaceValueAmount(faceAmount);
        entity.setQuantityValue(request.quantity());
        entity.setPayoutAmount(formatLocalAmount(country.currencySymbol(), localAmount));
        entity.setBaseAmountUsd(baseAmountUsd);
        entity.setLocalAmount(localAmount);
        entity.setEstimatedLocalAmount(localAmount);
        entity.setCurrencyCode(country.currencyCode());
        entity.setBusinessRateSnapshot(selectedQuote.localPayoutPerUnit());
        entity.setFaceToUsdRateSnapshot(faceToUsdRate);
        entity.setClientRequestId(clientRequestId);
        entity.setClientRequestHash(requestHash);
        entity.setStatusCode("PENDING");
        entity.setNote(note);
        entity.setVoucherImageUrl(normalizeNullable(request.voucherImageUrl()));
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        TradeOrderEntity saved = tradeOrderRepository.save(entity);
        persistentSupportService.appendUserOrderMessage(conversation, currentUser, sellOrderChatMessage(saved, request), saved);
        notificationService.notifyUser(
            assignedAgent,
            currentUser,
            "SELL_ORDER",
            "New sell order",
            currentUser.getUsername() + " submitted " + saved.getOrderNo(),
            "TRANSACTION",
            saved.getId()
        );
        notificationService.notifyAdmins(
            currentUser,
            "SELL_ORDER",
            "New sell order",
            currentUser.getUsername() + " submitted " + saved.getOrderNo(),
            "TRANSACTION",
            saved.getId()
        );

        return toTransactionItem(saved, currentUser.getId());
    }

    @Transactional
    public TransactionItem updateStatus(String transactionId, String nextStatus) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        TradeOrderEntity order = tradeOrderRepository.findByIdForUpdate(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (!canAccess(order, currentUser)) {
            throw new IllegalArgumentException("Transaction not accessible");
        }

        String normalizedStatus = normalizeStatus(nextStatus);
        validateTransition(order.getStatusCode(), normalizedStatus);

        order.setStatusCode(normalizedStatus.toUpperCase());
        order.setUpdatedAt(LocalDateTime.now());
        TradeOrderEntity saved = tradeOrderRepository.save(order);
        persistentSupportService.publishOrderUpdate(saved);
        return toTransactionItem(saved, currentUser.getId());
    }

    @Transactional
    public TransactionItem completeTransaction(String transactionId, CompleteTransactionRequest request) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        currentUserService.requireAgentOrAdmin(currentUser);

        TradeOrderEntity order = tradeOrderRepository.findByIdForUpdate(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));
        if (!canStaffOperate(order, currentUser)) {
            throw new IllegalArgumentException("Transaction not accessible");
        }
        if (!CANCELABLE_STATUSES.contains(order.getStatusCode().toUpperCase())) {
            throw new ConflictException("Only pending or processing orders can be completed");
        }

        BigDecimal finalAmount = money(request.finalLocalAmount());
        BigDecimal vipPoints = money(request.vipPoints());
        if (finalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Final local payout must be greater than zero");
        }
        if (vipPoints.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("VIP points cannot be negative");
        }

        BigDecimal estimatedAmount = estimatedAmount(order);
        String reason = normalizeSettlementReason(request.reason());
        if (estimatedAmount != null && finalAmount.compareTo(estimatedAmount) != 0 && reason.isBlank()) {
            throw new IllegalArgumentException("Settlement reason is required when final payout differs from estimate");
        }

        LocalDateTime now = LocalDateTime.now();
        order.setEstimatedLocalAmount(estimatedAmount);
        order.setFinalLocalAmount(finalAmount);
        order.setLocalAmount(finalAmount);
        order.setPayoutAmount(formatLocalAmount(
            countryCodeService.requireCountry(order.getOwnerUser().getCountryCode()).currencySymbol(),
            finalAmount
        ));
        order.setManualVipPoints(vipPoints);
        order.setSettlementReason(reason.isBlank() ? null : reason);
        order.setSettledByUser(currentUser);
        order.setSettledAt(now);
        order.setStatusCode("COMPLETED");
        order.setUpdatedAt(now);
        TradeOrderEntity saved = tradeOrderRepository.save(order);

        vipService.grantManualOrderPoints(saved, vipPoints);
        referralRewardService.rewardCompletedTrade(saved);
        saveSettlementAudit(saved, currentUser, "COMPLETED", estimatedAmount, finalAmount, vipPoints, reason, now);
        persistentSupportService.publishOrderUpdate(saved);
        return toTransactionItem(saved, currentUser.getId());
    }

    private TransactionItem toTransactionItem(TradeOrderEntity order, String currentUserId) {
        UserEntity counterpart = order.getOwnerUser().getId().equals(currentUserId)
            ? order.getCounterpartyUser()
            : order.getOwnerUser();

        return new TransactionItem(
            order.getId(),
            order.getOrderNo(),
            order.getCardName(),
            order.getFaceValue(),
            order.getFaceCurrencyCode() == null ? "" : order.getFaceCurrencyCode(),
            decimal(order.getFaceValueAmount()),
            order.getQuantityValue() == null ? 1 : order.getQuantityValue(),
            order.getPayoutAmount(),
            decimal(order.getBaseAmountUsd()),
            decimal(order.getLocalAmount()),
            decimal(order.getEstimatedLocalAmount() == null ? order.getLocalAmount() : order.getEstimatedLocalAmount()),
            decimal(order.getFinalLocalAmount()),
            order.getCurrencyCode() == null ? "" : order.getCurrencyCode(),
            decimal(order.getBusinessRateSnapshot()),
            decimal(order.getFaceToUsdRateSnapshot()),
            order.getStatusCode().toLowerCase(),
            counterpart.getUsername(),
            counterpart.getUsername(),
            order.getFriendship() == null ? "" : order.getFriendship().getId(),
            order.getNote() == null ? "" : order.getNote(),
            order.getVoucherImageUrl() == null ? "" : order.getVoucherImageUrl(),
            order.getCancelReason() == null ? "" : order.getCancelReason(),
            order.getCancelNote() == null ? "" : order.getCancelNote(),
            order.getCanceledByUser() == null ? "" : order.getCanceledByUser().getUsername(),
            order.getCanceledAt() == null ? "" : TIME_FORMATTER.format(order.getCanceledAt()),
            decimal(order.getManualVipPoints()),
            order.getSettlementReason() == null ? "" : order.getSettlementReason(),
            order.getSettledByUser() == null ? "" : order.getSettledByUser().getUsername(),
            order.getSettledAt() == null ? "" : TIME_FORMATTER.format(order.getSettledAt()),
            TIME_FORMATTER.format(order.getCreatedAt()),
            TIME_FORMATTER.format(order.getUpdatedAt())
        );
    }

    public List<CompletedTransactionFeedItem> getRecentCompletedTransactions() {
        currentUserService.getCurrentUser();
        return tradeOrderRepository.findTop12ByStatusCodeOrderByUpdatedAtDesc("COMPLETED").stream()
            .map(order -> new CompletedTransactionFeedItem(
                maskUsername(order.getOwnerUser().getUsername()),
                order.getCardName(),
                order.getPayoutAmount(),
                TIME_FORMATTER.format(order.getUpdatedAt())
            ))
            .toList();
    }

    @Transactional
    public TransactionItem cancelTransaction(String transactionId, String reason, String note, Boolean notifyCustomer) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        currentUserService.requireAgentOrAdmin(currentUser);

        TradeOrderEntity order = tradeOrderRepository.findByIdForUpdate(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (!canAccess(order, currentUser)) {
            throw new IllegalArgumentException("Transaction not accessible");
        }
        if (!CANCELABLE_STATUSES.contains(order.getStatusCode().toUpperCase())) {
            throw new IllegalArgumentException("Only pending or processing orders can be canceled");
        }

        String normalizedReason = requireReason(reason);
        String normalizedNote = normalizeCancelNote(note);
        order.setStatusCode("CANCELED");
        order.setCancelReason(normalizedReason);
        order.setCancelNote(normalizedNote);
        order.setCanceledByUser(currentUser);
        order.setCanceledAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        TradeOrderEntity saved = tradeOrderRepository.save(order);

        saveSettlementAudit(
            saved,
            currentUser,
            "CANCELED",
            estimatedAmount(saved),
            null,
            BigDecimal.ZERO.setScale(2),
            normalizedReason + (normalizedNote.isBlank() ? "" : ": " + normalizedNote),
            saved.getCanceledAt()
        );
        persistentSupportService.publishOrderUpdate(saved);

        if (notifyCustomer == null || notifyCustomer) {
            SupportConversationEntity conversation = persistentSupportService.ensureUserConversation(order.getOwnerUser());
            if (conversation != null) {
                persistentSupportService.appendSystemMessage(
                    conversation,
                    "Order %s was canceled. Reason: %s%s".formatted(
                        saved.getOrderNo(),
                        normalizedReason,
                        normalizedNote.isBlank() ? "" : ". Note: " + normalizedNote
                    )
                );
            }
        }
        return toTransactionItem(saved, currentUser.getId());
    }

    private boolean canAccess(TradeOrderEntity order, UserEntity currentUser) {
        return isAdmin(currentUser)
            || order.getOwnerUser().getId().equals(currentUser.getId())
            || order.getCounterpartyUser().getId().equals(currentUser.getId())
            || canAgentAccessCustomerOrder(order, currentUser);
    }

    private boolean isAdmin(UserEntity user) {
        return "ADMIN".equalsIgnoreCase(user.getRoleCode());
    }

    private boolean canStaffOperate(TradeOrderEntity order, UserEntity currentUser) {
        return isAdmin(currentUser) || canAgentAccessCustomerOrder(order, currentUser);
    }

    private boolean shouldShowToCurrentUser(TradeOrderEntity order, UserEntity currentUser) {
        if (!"USER".equalsIgnoreCase(currentUser.getRoleCode())) {
            return true;
        }
        return !userHiddenRecordService.isHidden(
            currentUser.getId(),
            UserHiddenRecordService.TYPE_ORDER,
            order.getId(),
            "ORDER"
        );
    }

    private boolean canAgentAccessCustomerOrder(TradeOrderEntity order, UserEntity currentUser) {
        return "AGENT".equalsIgnoreCase(currentUser.getRoleCode())
            && supportConversationRepository.existsByCustomerUser_IdAndAssignedAgent_Id(order.getOwnerUser().getId(), currentUser.getId());
    }

    private String normalizeStatus(String nextStatus) {
        if (nextStatus == null) {
            throw new IllegalArgumentException("Status is required");
        }

        String normalized = nextStatus.trim().toLowerCase();
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported transaction status");
        }
        return normalized;
    }

    private void validateTransition(String currentStatusCode, String nextStatus) {
        String current = currentStatusCode.toLowerCase();
        if (current.equals(nextStatus)) {
            return;
        }

        boolean valid = switch (current) {
            case "pending" -> nextStatus.equals("processing") || nextStatus.equals("completed") || nextStatus.equals("disputed");
            case "processing" -> nextStatus.equals("completed") || nextStatus.equals("disputed");
            case "completed", "disputed", "canceled" -> false;
            default -> false;
        };

        if (!valid) {
            throw new IllegalArgumentException("Invalid transaction status transition");
        }
    }

    private FriendshipEntity resolveFriendship(String currentUserId, String counterpartyUserId, String friendshipId) {
        if (StringUtils.hasText(friendshipId)) {
            FriendshipEntity friendship = friendshipRepository.findById(friendshipId.trim()).orElse(null);
            if (friendship != null && isParticipant(friendship, currentUserId, counterpartyUserId)) {
                return friendship;
            }
        }

        FriendshipEntity forward = friendshipRepository.findByRequesterUser_IdAndAddresseeUser_Id(currentUserId, counterpartyUserId).orElse(null);
        if (forward != null) {
            return forward;
        }

        return friendshipRepository.findByRequesterUser_IdAndAddresseeUser_Id(counterpartyUserId, currentUserId).orElse(null);
    }

    private boolean isParticipant(FriendshipEntity friendship, String currentUserId, String counterpartyUserId) {
        return (friendship.getRequesterUser().getId().equals(currentUserId) && friendship.getAddresseeUser().getId().equals(counterpartyUserId))
            || (friendship.getRequesterUser().getId().equals(counterpartyUserId) && friendship.getAddresseeUser().getId().equals(currentUserId));
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeFaceCurrency(String value) {
        String normalized = StringUtils.hasText(value)
            ? value.trim().replaceAll("[\\s_-]+", "").toUpperCase(java.util.Locale.ROOT)
            : "";
        return switch (normalized) {
            case "US", "USA", "UNITEDSTATES" -> "USD";
            default -> normalized;
        };
    }

    private String normalizeClientRequestId(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.length() > 64 || normalized.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Invalid client request id");
        }
        return normalized;
    }

    private BigDecimal estimatedAmount(TradeOrderEntity order) {
        return order.getEstimatedLocalAmount() == null ? order.getLocalAmount() : order.getEstimatedLocalAmount();
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeSettlementReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            return "";
        }
        String normalized = reason.trim();
        if (normalized.length() > 255) {
            throw new IllegalArgumentException("Settlement reason must be 255 characters or less");
        }
        return normalized;
    }

    private void saveSettlementAudit(
        TradeOrderEntity order,
        UserEntity operator,
        String action,
        BigDecimal estimatedAmount,
        BigDecimal finalAmount,
        BigDecimal vipPoints,
        String reason,
        LocalDateTime createdAt
    ) {
        if (tradeOrderSettlementAuditRepository.existsByTradeOrder_IdAndActionCode(order.getId(), action)) {
            throw new ConflictException("Order action was already recorded");
        }
        TradeOrderSettlementAuditEntity audit = new TradeOrderSettlementAuditEntity();
        audit.setId(UUID.randomUUID().toString());
        audit.setTradeOrder(order);
        audit.setOperatorUser(operator);
        audit.setActionCode(action);
        audit.setEstimatedLocalAmount(estimatedAmount);
        audit.setFinalLocalAmount(finalAmount);
        audit.setCurrencyCode(order.getCurrencyCode());
        audit.setVipPoints(money(vipPoints));
        String normalizedReason = StringUtils.hasText(reason) ? reason.trim() : null;
        audit.setReasonNote(normalizedReason != null && normalizedReason.length() > 255
            ? normalizedReason.substring(0, 255)
            : normalizedReason);
        audit.setCreatedAt(createdAt == null ? LocalDateTime.now() : createdAt);
        tradeOrderSettlementAuditRepository.save(audit);
    }

    private String sellOrderRequestHash(CreateSellOrderRequest request) {
        String canonical = String.join("\u001f",
            request.cardName().trim(),
            normalizeFaceCurrency(request.cardCountry()),
            request.settlementCountry().trim(),
            BigDecimal.valueOf(request.faceValue()).stripTrailingZeros().toPlainString(),
            String.valueOf(request.quantity()),
            request.cardType().trim(),
            request.speed().trim(),
            value(request.cardData()),
            value(request.note()),
            value(request.voucherImageUrl()),
            String.valueOf(request.sendChatMessage() == null || request.sendChatMessage())
        );
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String value(String value) {
        return value == null ? "" : value.trim();
    }

    private String requireReason(String reason) {
        if (!StringUtils.hasText(reason)) {
            throw new IllegalArgumentException("Cancel reason is required");
        }
        String normalized = reason.trim();
        return normalized.length() > 64 ? normalized.substring(0, 64) : normalized;
    }

    private String normalizeCancelNote(String note) {
        if (!StringUtils.hasText(note)) {
            return "";
        }
        String normalized = note.trim();
        return normalized.length() > 255 ? normalized.substring(0, 255) : normalized;
    }

    private String formatFaceValue(double faceValue, String cardCountry, int quantity) {
        String amount = faceValue == Math.rint(faceValue)
            ? String.valueOf((long) faceValue)
            : String.valueOf(faceValue);
        return amount + " " + cardCountry.trim() + " x" + quantity;
    }

    private String sellOrderNote(CreateSellOrderRequest request, BigDecimal businessRate) {
        StringBuilder note = new StringBuilder();
        note.append("Sell card order. Type: ")
            .append(request.cardType().trim())
            .append(", speed: ")
            .append(request.speed().trim())
            .append(", settlement country: ")
            .append(countryCodeService.requireCountry(currentUserService.getCurrentUser().getCountryCode()).countryName())
            .append(", rate: ")
            .append(decimal(businessRate));
        if (StringUtils.hasText(request.cardData())) {
            note.append(", card data: ").append(request.cardData().trim());
        }
        if (StringUtils.hasText(request.note())) {
            note.append(", note: ").append(request.note().trim());
        }
        return note.length() > 255 ? note.substring(0, 255) : note.toString();
    }

    private String sellOrderChatMessage(TradeOrderEntity order, CreateSellOrderRequest request) {
        return """
            Sell order %s
            Card: %s
            Country: %s
            Settlement country: %s
            Face value: %s
            Type: %s / %s
            Rate: %s
            Settlement: %s
            """.formatted(
            order.getOrderNo(),
            order.getCardName(),
            request.cardCountry().trim(),
            countryCodeService.requireCountry(order.getOwnerUser().getCountryCode()).countryName(),
            order.getFaceValue(),
            request.cardType().trim(),
            request.speed().trim(),
            decimal(order.getBusinessRateSnapshot()),
            order.getPayoutAmount()
        ).trim();
    }

    private String decimal(BigDecimal amount) {
        return amount == null ? "" : amount.stripTrailingZeros().toPlainString();
    }

    private String maskUsername(String username) {
        if (!StringUtils.hasText(username)) {
            return "***";
        }
        String normalized = username.trim();
        if (normalized.length() == 1) {
            return normalized + "***";
        }
        if (normalized.length() == 2) {
            return normalized.charAt(0) + "***";
        }
        return normalized.charAt(0) + "***" + normalized.charAt(normalized.length() - 1);
    }

    private String formatLocalAmount(String symbol, BigDecimal amount) {
        return symbol + (symbol.length() > 1 ? " " : "") + amount.stripTrailingZeros().toPlainString();
    }
}
