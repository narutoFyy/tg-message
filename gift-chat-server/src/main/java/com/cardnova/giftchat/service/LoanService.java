package com.cardnova.giftchat.service;

import com.cardnova.giftchat.dto.CreateLoanApplicationRequest;
import com.cardnova.giftchat.entity.LoanApplicationEntity;
import com.cardnova.giftchat.entity.SupportConversationEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.model.LoanApplicationItem;
import com.cardnova.giftchat.repository.LoanApplicationRepository;
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
public class LoanService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final Set<String> ALLOWED_STATUSES = Set.of("pending", "approved", "rejected");

    private final LoanApplicationRepository loanApplicationRepository;
    private final CurrentUserService currentUserService;
    private final PersistentSupportService persistentSupportService;
    private final NotificationService notificationService;

    public LoanService(
        LoanApplicationRepository loanApplicationRepository,
        CurrentUserService currentUserService,
        PersistentSupportService persistentSupportService,
        NotificationService notificationService
    ) {
        this.loanApplicationRepository = loanApplicationRepository;
        this.currentUserService = currentUserService;
        this.persistentSupportService = persistentSupportService;
        this.notificationService = notificationService;
    }

    public List<LoanApplicationItem> getLoans() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        if ("ADMIN".equalsIgnoreCase(currentUser.getRoleCode())) {
            return loanApplicationRepository.findAllByOrderByUpdatedAtDesc().stream().map(this::toItem).toList();
        }
        if ("AGENT".equalsIgnoreCase(currentUser.getRoleCode())) {
            return loanApplicationRepository.findByAssignedAgent_IdOrderByUpdatedAtDesc(currentUser.getId()).stream().map(this::toItem).toList();
        }
        return loanApplicationRepository.findByOwnerUser_IdOrderByUpdatedAtDesc(currentUser.getId()).stream().map(this::toItem).toList();
    }

    @Transactional
    public LoanApplicationItem create(CreateLoanApplicationRequest request) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        if (!"USER".equalsIgnoreCase(currentUser.getRoleCode())) {
            throw new IllegalArgumentException("Only users can apply for loans");
        }

        SupportConversationEntity conversation = persistentSupportService.ensureUserConversation(currentUser);
        UserEntity assignedAgent = conversation == null ? null : conversation.getAssignedAgent();
        if (assignedAgent == null) {
            throw new IllegalArgumentException("No active support agent available");
        }

        LoanApplicationEntity entity = new LoanApplicationEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setApplicationNo(nextApplicationNo());
        entity.setOwnerUser(currentUser);
        entity.setAssignedAgent(assignedAgent);
        entity.setAmount(request.amount().trim());
        entity.setCountry(request.country().trim());
        entity.setPurpose(request.purpose().trim());
        entity.setContact(normalizeNullable(request.contact()));
        entity.setRepaymentPlan(normalizeNullable(request.repaymentPlan()));
        entity.setStatusCode("PENDING");
        entity.setReviewNote(null);
        entity.setReviewedBy(null);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        LoanApplicationEntity saved = loanApplicationRepository.save(entity);

        String message = """
            Loan application %s
            Amount: %s
            Country: %s
            Purpose: %s
            Contact: %s
            Repayment plan: %s
            """.formatted(
            saved.getApplicationNo(),
            saved.getAmount(),
            saved.getCountry(),
            saved.getPurpose(),
            value(saved.getContact()),
            value(saved.getRepaymentPlan())
        ).trim();
        if (request.sendChatMessage() == null || request.sendChatMessage()) {
            persistentSupportService.appendSystemMessage(conversation, message);
        }
        notificationService.notifyUser(
            assignedAgent,
            currentUser,
            "LOAN",
            "New loan application",
            currentUser.getUsername() + " submitted " + saved.getApplicationNo(),
            "LOAN",
            saved.getId()
        );
        notificationService.notifyAdmins(
            currentUser,
            "LOAN",
            "New loan application",
            currentUser.getUsername() + " submitted " + saved.getApplicationNo(),
            "LOAN",
            saved.getId()
        );

        return toItem(saved);
    }

    @Transactional
    public LoanApplicationItem updateStatus(String loanId, String status, String reviewNote) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        currentUserService.requireAdmin(currentUser);

        String normalized = status.trim().toLowerCase();
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Unsupported loan status");
        }

        LoanApplicationEntity entity = loanApplicationRepository.findById(loanId)
            .orElseThrow(() -> new IllegalArgumentException("Loan application not found"));
        entity.setStatusCode(normalized.toUpperCase());
        entity.setReviewNote(normalizeNullable(reviewNote));
        entity.setReviewedBy(currentUser);
        entity.setUpdatedAt(LocalDateTime.now());
        LoanApplicationEntity saved = loanApplicationRepository.save(entity);

        SupportConversationEntity conversation = persistentSupportService.ensureUserConversation(saved.getOwnerUser());
        String reviewLine = StringUtils.hasText(saved.getReviewNote()) ? "\nNote: " + saved.getReviewNote() : "";
        persistentSupportService.appendStaffMessage(
            conversation,
            currentUser,
            "ADMIN",
            "TEXT",
            "Loan application %s is %s.%s".formatted(saved.getApplicationNo(), normalized, reviewLine)
        );
        notificationService.notifyUser(
            saved.getOwnerUser(),
            currentUser,
            "LOAN_REVIEW",
            "Loan review updated",
            saved.getApplicationNo() + " is " + normalized,
            "LOAN",
            saved.getId()
        );
        if (saved.getAssignedAgent() != null) {
            notificationService.notifyUser(
                saved.getAssignedAgent(),
                currentUser,
                "LOAN_REVIEW",
                "Loan review updated",
                saved.getApplicationNo() + " is " + normalized,
                "LOAN",
                saved.getId()
            );
        }

        return toItem(saved);
    }

    private LoanApplicationItem toItem(LoanApplicationEntity entity) {
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

    private String nextApplicationNo() {
        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();
        long count = loanApplicationRepository.countByCreatedAtBetween(start, end) + 1;
        return "LN" + today.format(DateTimeFormatter.ofPattern("yyMMdd")) + "-" + String.format("%03d", count);
    }

    private String normalizeNullable(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
