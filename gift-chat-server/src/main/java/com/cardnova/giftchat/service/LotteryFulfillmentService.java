package com.cardnova.giftchat.service;

import com.cardnova.giftchat.dto.CreateLotteryFulfillmentRequest;
import com.cardnova.giftchat.entity.LotteryDrawRecordEntity;
import com.cardnova.giftchat.entity.LotteryFulfillmentOrderEntity;
import com.cardnova.giftchat.entity.SupportConversationEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.model.LotteryFulfillmentItem;
import com.cardnova.giftchat.repository.LotteryDrawRecordRepository;
import com.cardnova.giftchat.repository.LotteryFulfillmentOrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class LotteryFulfillmentService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Set<String> ALLOWED_STATUSES = Set.of("pending", "completed");

    private final LotteryFulfillmentOrderRepository orderRepository;
    private final LotteryDrawRecordRepository drawRecordRepository;
    private final CurrentUserService currentUserService;
    private final PersistentSupportService persistentSupportService;
    private final NotificationService notificationService;

    public LotteryFulfillmentService(
        LotteryFulfillmentOrderRepository orderRepository,
        LotteryDrawRecordRepository drawRecordRepository,
        CurrentUserService currentUserService,
        PersistentSupportService persistentSupportService,
        NotificationService notificationService
    ) {
        this.orderRepository = orderRepository;
        this.drawRecordRepository = drawRecordRepository;
        this.currentUserService = currentUserService;
        this.persistentSupportService = persistentSupportService;
        this.notificationService = notificationService;
    }

    public List<LotteryFulfillmentItem> orders() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        List<LotteryFulfillmentOrderEntity> orders;
        if ("ADMIN".equalsIgnoreCase(currentUser.getRoleCode())) {
            orders = orderRepository.findAllByOrderByUpdatedAtDesc();
        } else if ("AGENT".equalsIgnoreCase(currentUser.getRoleCode())) {
            orders = orderRepository.findByAssignedAgent_IdOrderByUpdatedAtDesc(currentUser.getId());
        } else {
            orders = orderRepository.findByOwnerUser_IdOrderByUpdatedAtDesc(currentUser.getId());
        }
        return orders.stream().map(this::toItem).toList();
    }

    @Transactional
    public LotteryFulfillmentItem create(String recordId, CreateLotteryFulfillmentRequest request) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        if (!"USER".equalsIgnoreCase(currentUser.getRoleCode())) {
            throw new IllegalArgumentException("Only users can claim physical prizes");
        }
        LotteryDrawRecordEntity record = drawRecordRepository.findById(recordId)
            .orElseThrow(() -> new IllegalArgumentException("Lottery record not found"));
        if (!record.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Lottery record not accessible");
        }
        if (!"PHYSICAL".equalsIgnoreCase(record.getPrize().getPrizeType())) {
            throw new IllegalArgumentException("Cash prizes require a bank withdrawal");
        }
        if (orderRepository.existsByLotteryDrawRecord_Id(recordId)) {
            throw new IllegalArgumentException("Prize fulfillment order already exists");
        }

        SupportConversationEntity conversation = persistentSupportService.ensureUserConversation(currentUser);
        UserEntity assignedAgent = conversation == null ? null : conversation.getAssignedAgent();
        if (assignedAgent == null) {
            throw new IllegalArgumentException("No active support agent available");
        }

        LocalDateTime now = LocalDateTime.now();
        LotteryFulfillmentOrderEntity order = new LotteryFulfillmentOrderEntity();
        order.setId(UUID.randomUUID().toString());
        order.setOrderNo(nextOrderNo());
        order.setOwnerUser(currentUser);
        order.setAssignedAgent(assignedAgent);
        order.setLotteryDrawRecord(record);
        order.setRecipientName(request.recipientName().trim());
        order.setPhone(request.phone().trim());
        order.setCountry(request.country().trim());
        order.setAddressLine(request.addressLine().trim());
        order.setStatusCode("PENDING");
        order.setCreatedAt(now);
        order.setUpdatedAt(now);
        LotteryFulfillmentOrderEntity saved = orderRepository.save(order);

        record.setFulfillmentStatus("PROCESSING");
        record.setProcessedAt(now);
        drawRecordRepository.save(record);
        persistentSupportService.appendUserOrderMessage(conversation, currentUser, userOrderMessage(saved));
        notifySupport(currentUser, assignedAgent, saved);
        return toItem(saved);
    }

    @Transactional
    public LotteryFulfillmentItem updateStatus(String orderId, String status) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        currentUserService.requireAgentOrAdmin(currentUser);
        String normalized = status == null ? "" : status.trim().toLowerCase(Locale.ROOT);
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported fulfillment status");
        }
        LotteryFulfillmentOrderEntity order = orderRepository.findById(orderId)
            .orElseThrow(() -> new IllegalArgumentException("Prize fulfillment order not found"));
        if ("AGENT".equalsIgnoreCase(currentUser.getRoleCode())
            && (order.getAssignedAgent() == null || !order.getAssignedAgent().getId().equals(currentUser.getId()))) {
            throw new IllegalArgumentException("Prize fulfillment order not accessible");
        }

        order.setStatusCode(normalized.toUpperCase(Locale.ROOT));
        order.setUpdatedAt(LocalDateTime.now());
        LotteryFulfillmentOrderEntity saved = orderRepository.save(order);
        if ("COMPLETED".equalsIgnoreCase(saved.getStatusCode())) {
            LotteryDrawRecordEntity record = saved.getLotteryDrawRecord();
            record.setFulfillmentStatus("FULFILLED");
            record.setProcessedBy(currentUser);
            record.setProcessedAt(LocalDateTime.now());
            drawRecordRepository.save(record);
        }
        return toItem(saved);
    }

    private String nextOrderNo() {
        LocalDate today = LocalDate.now();
        long count = orderRepository.countByCreatedAtBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay()) + 1;
        return "PF" + today.format(DateTimeFormatter.ofPattern("yyMMdd")) + "-" + String.format("%03d", count);
    }

    private String userOrderMessage(LotteryFulfillmentOrderEntity order) {
        return "I submitted a delivery request for my %s prize (order %s). Recipient: %s; phone: %s; country: %s; address: %s."
            .formatted(
                order.getLotteryDrawRecord().getPrize().getName(),
                order.getOrderNo(),
                order.getRecipientName(),
                order.getPhone(),
                order.getCountry(),
                order.getAddressLine()
            );
    }

    private void notifySupport(UserEntity user, UserEntity assignedAgent, LotteryFulfillmentOrderEntity order) {
        String body = user.getUsername() + " submitted " + order.getOrderNo();
        notificationService.notifyUser(assignedAgent, user, "LOTTERY_FULFILLMENT", "New physical prize claim", body, "LOTTERY_FULFILLMENT", order.getId());
        notificationService.notifyAdmins(user, "LOTTERY_FULFILLMENT", "New physical prize claim", body, "LOTTERY_FULFILLMENT", order.getId());
    }

    private LotteryFulfillmentItem toItem(LotteryFulfillmentOrderEntity order) {
        LotteryDrawRecordEntity record = order.getLotteryDrawRecord();
        return new LotteryFulfillmentItem(
            order.getId(),
            order.getOrderNo(),
            order.getOwnerUser().getUsername(),
            record.getId(),
            record.getPrize().getName(),
            record.getPrize().getPrizeType().toLowerCase(Locale.ROOT),
            order.getRecipientName(),
            order.getPhone(),
            order.getCountry(),
            order.getAddressLine(),
            order.getStatusCode().toLowerCase(Locale.ROOT),
            order.getAssignedAgent() == null ? "" : order.getAssignedAgent().getUsername(),
            TIME_FORMATTER.format(order.getCreatedAt()),
            TIME_FORMATTER.format(order.getUpdatedAt())
        );
    }
}
