package com.cardnova.giftchat.service;

import com.cardnova.giftchat.dto.CreateWithdrawalRequest;
import com.cardnova.giftchat.entity.SupportConversationEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.entity.WithdrawalRequestEntity;
import com.cardnova.giftchat.model.WithdrawalItem;
import com.cardnova.giftchat.repository.WithdrawalRequestRepository;
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
public class WithdrawalService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Set<String> ALLOWED_STATUSES = Set.of("pending", "completed");

    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final CurrentUserService currentUserService;
    private final PersistentSupportService persistentSupportService;
    private final NotificationService notificationService;

    public WithdrawalService(
        WithdrawalRequestRepository withdrawalRequestRepository,
        CurrentUserService currentUserService,
        PersistentSupportService persistentSupportService,
        NotificationService notificationService
    ) {
        this.withdrawalRequestRepository = withdrawalRequestRepository;
        this.currentUserService = currentUserService;
        this.persistentSupportService = persistentSupportService;
        this.notificationService = notificationService;
    }

    public List<WithdrawalItem> getWithdrawals() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        if ("ADMIN".equalsIgnoreCase(currentUser.getRoleCode())) {
            return withdrawalRequestRepository.findAllByOrderByUpdatedAtDesc().stream().map(this::toItem).toList();
        }
        if ("AGENT".equalsIgnoreCase(currentUser.getRoleCode())) {
            return withdrawalRequestRepository.findByAssignedAgent_IdOrderByUpdatedAtDesc(currentUser.getId()).stream().map(this::toItem).toList();
        }
        return withdrawalRequestRepository.findByOwnerUser_IdOrderByUpdatedAtDesc(currentUser.getId()).stream().map(this::toItem).toList();
    }

    @Transactional
    public WithdrawalItem create(CreateWithdrawalRequest request) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        if (!"USER".equalsIgnoreCase(currentUser.getRoleCode())) {
            throw new IllegalArgumentException("Only users can withdraw");
        }

        SupportConversationEntity conversation = persistentSupportService.ensureUserConversation(currentUser);
        UserEntity assignedAgent = conversation == null ? null : conversation.getAssignedAgent();
        if (assignedAgent == null) {
            throw new IllegalArgumentException("No active support agent available");
        }

        WithdrawalRequestEntity entity = new WithdrawalRequestEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setRequestNo(nextRequestNo());
        entity.setOwnerUser(currentUser);
        entity.setAssignedAgent(assignedAgent);
        entity.setAmount(request.amount().trim());
        entity.setCountry(request.country().trim());
        entity.setAccountName(request.accountName().trim());
        entity.setBankName(request.bankName().trim());
        entity.setAccountNumber(request.accountNumber().trim());
        entity.setContact(normalizeNullable(request.contact()));
        entity.setNote(normalizeNullable(request.note()));
        entity.setStatusCode("PENDING");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        WithdrawalRequestEntity saved = withdrawalRequestRepository.save(entity);

        String message = """
            Withdrawal request %s
            Amount: %s
            Country: %s
            Account: %s
            Bank: %s
            Number: %s
            """.formatted(
            saved.getRequestNo(),
            saved.getAmount(),
            saved.getCountry(),
            saved.getAccountName(),
            saved.getBankName(),
            saved.getAccountNumber()
        ).trim();
        persistentSupportService.appendSystemMessage(conversation, message);
        notificationService.notifyUser(
            assignedAgent,
            currentUser,
            "WITHDRAWAL",
            "New withdrawal request",
            currentUser.getUsername() + " submitted " + saved.getRequestNo(),
            "WITHDRAWAL",
            saved.getId()
        );
        notificationService.notifyAdmins(
            currentUser,
            "WITHDRAWAL",
            "New withdrawal request",
            currentUser.getUsername() + " submitted " + saved.getRequestNo(),
            "WITHDRAWAL",
            saved.getId()
        );

        return toItem(saved);
    }

    @Transactional
    public WithdrawalItem updateStatus(String withdrawalId, String status) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        if (!"AGENT".equalsIgnoreCase(currentUser.getRoleCode()) && !"ADMIN".equalsIgnoreCase(currentUser.getRoleCode())) {
            throw new IllegalArgumentException("Only support or admin can update withdrawals");
        }

        String normalized = status.trim().toLowerCase();
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported withdrawal status");
        }

        WithdrawalRequestEntity entity = withdrawalRequestRepository.findById(withdrawalId)
            .orElseThrow(() -> new IllegalArgumentException("Withdrawal not found"));
        if ("AGENT".equalsIgnoreCase(currentUser.getRoleCode())
            && (entity.getAssignedAgent() == null || !entity.getAssignedAgent().getId().equals(currentUser.getId()))) {
            throw new IllegalArgumentException("Withdrawal not accessible");
        }

        entity.setStatusCode(normalized.toUpperCase());
        entity.setUpdatedAt(LocalDateTime.now());
        return toItem(withdrawalRequestRepository.save(entity));
    }

    private WithdrawalItem toItem(WithdrawalRequestEntity entity) {
        return new WithdrawalItem(
            entity.getId(),
            entity.getRequestNo(),
            entity.getOwnerUser().getUsername(),
            entity.getAmount(),
            entity.getCountry(),
            entity.getAccountName(),
            entity.getBankName(),
            entity.getAccountNumber(),
            entity.getContact() == null ? "" : entity.getContact(),
            entity.getNote() == null ? "" : entity.getNote(),
            entity.getStatusCode().toLowerCase(),
            entity.getAssignedAgent() == null ? "" : entity.getAssignedAgent().getUsername(),
            TIME_FORMATTER.format(entity.getCreatedAt()),
            TIME_FORMATTER.format(entity.getUpdatedAt())
        );
    }

    private String nextRequestNo() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        long count = withdrawalRequestRepository.countByCreatedAtBetween(start, end) + 1;
        return "WD" + today.format(DateTimeFormatter.ofPattern("yyMMdd")) + "-" + String.format("%03d", count);
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
