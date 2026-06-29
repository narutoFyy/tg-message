package com.cardnova.giftchat.service;

import com.cardnova.giftchat.dto.CreateTransactionRequest;
import com.cardnova.giftchat.dto.CreateSellOrderRequest;
import com.cardnova.giftchat.entity.SupportConversationEntity;
import com.cardnova.giftchat.entity.FriendshipEntity;
import com.cardnova.giftchat.entity.TradeOrderEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.model.TransactionItem;
import com.cardnova.giftchat.repository.BlacklistEntryRepository;
import com.cardnova.giftchat.repository.FriendshipRepository;
import com.cardnova.giftchat.repository.SupportConversationRepository;
import com.cardnova.giftchat.repository.TradeOrderRepository;
import com.cardnova.giftchat.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PersistentTransactionService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Set<String> ALLOWED_STATUSES = Set.of("pending", "processing", "completed", "disputed");

    private final TradeOrderRepository tradeOrderRepository;
    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final FriendshipRepository friendshipRepository;
    private final BlacklistEntryRepository blacklistEntryRepository;
    private final PersistentSupportService persistentSupportService;
    private final NotificationService notificationService;
    private final SupportConversationRepository supportConversationRepository;

    public PersistentTransactionService(
        TradeOrderRepository tradeOrderRepository,
        CurrentUserService currentUserService,
        UserRepository userRepository,
        FriendshipRepository friendshipRepository,
        BlacklistEntryRepository blacklistEntryRepository,
        PersistentSupportService persistentSupportService,
        NotificationService notificationService,
        SupportConversationRepository supportConversationRepository
    ) {
        this.tradeOrderRepository = tradeOrderRepository;
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.friendshipRepository = friendshipRepository;
        this.blacklistEntryRepository = blacklistEntryRepository;
        this.persistentSupportService = persistentSupportService;
        this.notificationService = notificationService;
        this.supportConversationRepository = supportConversationRepository;
    }

    public List<TransactionItem> getTransactions() {
        UserEntity currentUser = currentUserService.getCurrentUser();

        if (isAdmin(currentUser)) {
            return tradeOrderRepository.findAllByOrderByUpdatedAtDesc().stream()
                .map(order -> toTransactionItem(order, currentUser.getId()))
                .toList();
        }

        return tradeOrderRepository.findByOwnerUser_IdOrCounterpartyUser_IdOrderByUpdatedAtDesc(currentUser.getId(), currentUser.getId()).stream()
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
        entity.setOrderNo(nextOrderNo());
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

        SupportConversationEntity conversation = persistentSupportService.ensureUserConversation(currentUser);
        UserEntity assignedAgent = conversation == null ? null : conversation.getAssignedAgent();
        if (assignedAgent == null) {
            throw new IllegalArgumentException("No active support agent available");
        }

        String faceValue = formatFaceValue(request.faceValue(), request.cardCountry(), request.quantity());
        String note = sellOrderNote(request);

        TradeOrderEntity entity = new TradeOrderEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setOrderNo(nextOrderNo());
        entity.setOwnerUser(currentUser);
        entity.setCounterpartyUser(assignedAgent);
        entity.setFriendship(null);
        entity.setCardName(request.cardName().trim());
        entity.setFaceValue(faceValue);
        entity.setPayoutAmount(request.settlementAmount().trim());
        entity.setStatusCode("PENDING");
        entity.setNote(note);
        entity.setVoucherImageUrl(normalizeNullable(request.voucherImageUrl()));
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());

        TradeOrderEntity saved = tradeOrderRepository.save(entity);
        if (request.sendChatMessage() == null || request.sendChatMessage()) {
            persistentSupportService.appendSystemMessage(conversation, sellOrderChatMessage(saved, request));
        }
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
        TradeOrderEntity order = tradeOrderRepository.findById(transactionId)
            .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        if (!canAccess(order, currentUser)) {
            throw new IllegalArgumentException("Transaction not accessible");
        }

        String normalizedStatus = normalizeStatus(nextStatus);
        validateTransition(order.getStatusCode(), normalizedStatus);

        order.setStatusCode(normalizedStatus.toUpperCase());
        order.setUpdatedAt(LocalDateTime.now());
        TradeOrderEntity saved = tradeOrderRepository.save(order);
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
            order.getPayoutAmount(),
            order.getStatusCode().toLowerCase(),
            counterpart.getUsername(),
            counterpart.getUsername(),
            order.getFriendship() == null ? "" : order.getFriendship().getId(),
            order.getNote() == null ? "" : order.getNote(),
            order.getVoucherImageUrl() == null ? "" : order.getVoucherImageUrl(),
            TIME_FORMATTER.format(order.getCreatedAt()),
            TIME_FORMATTER.format(order.getUpdatedAt())
        );
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
            case "pending" -> nextStatus.equals("processing") || nextStatus.equals("disputed");
            case "processing" -> nextStatus.equals("completed") || nextStatus.equals("disputed");
            case "completed", "disputed" -> false;
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

    private String nextOrderNo() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        long count = tradeOrderRepository.countByCreatedAtBetween(start, end) + 1;
        return "CB" + today.format(DateTimeFormatter.ofPattern("yyMMdd")) + "-" + String.format("%03d", count);
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String formatFaceValue(double faceValue, String cardCountry, int quantity) {
        String amount = faceValue == Math.rint(faceValue)
            ? String.valueOf((long) faceValue)
            : String.valueOf(faceValue);
        return amount + " " + cardCountry.trim() + " x" + quantity;
    }

    private String sellOrderNote(CreateSellOrderRequest request) {
        StringBuilder note = new StringBuilder();
        note.append("Sell card order. Type: ")
            .append(request.cardType().trim())
            .append(", speed: ")
            .append(request.speed().trim())
            .append(", settlement country: ")
            .append(request.settlementCountry().trim())
            .append(", rate: ")
            .append(request.rate().trim());
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
            request.settlementCountry().trim(),
            order.getFaceValue(),
            request.cardType().trim(),
            request.speed().trim(),
            request.rate().trim(),
            order.getPayoutAmount()
        ).trim();
    }
}
