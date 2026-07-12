package com.cardnova.giftchat.service;

import com.cardnova.giftchat.dto.BindBankAccountRequest;
import com.cardnova.giftchat.dto.CreateWithdrawalRequest;
import com.cardnova.giftchat.dto.CreateLotteryCashClaimRequest;
import com.cardnova.giftchat.entity.LotteryDrawRecordEntity;
import com.cardnova.giftchat.entity.SupportConversationEntity;
import com.cardnova.giftchat.entity.UserBankAccountEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.entity.WithdrawalRequestEntity;
import com.cardnova.giftchat.model.WithdrawalItem;
import com.cardnova.giftchat.repository.LotteryDrawRecordRepository;
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
    private final BankAccountService bankAccountService;
    private final LotteryDrawRecordRepository lotteryDrawRecordRepository;

    public WithdrawalService(
        WithdrawalRequestRepository withdrawalRequestRepository,
        CurrentUserService currentUserService,
        PersistentSupportService persistentSupportService,
        NotificationService notificationService,
        BankAccountService bankAccountService,
        LotteryDrawRecordRepository lotteryDrawRecordRepository
    ) {
        this.withdrawalRequestRepository = withdrawalRequestRepository;
        this.currentUserService = currentUserService;
        this.persistentSupportService = persistentSupportService;
        this.notificationService = notificationService;
        this.bankAccountService = bankAccountService;
        this.lotteryDrawRecordRepository = lotteryDrawRecordRepository;
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

        BindBankAccountRequest bankRequest = new BindBankAccountRequest(
            request.country(),
            request.accountName(),
            request.bankName(),
            request.accountNumber()
        );
        UserBankAccountEntity bankAccount = bankAccountForWithdrawal(currentUser, bankRequest);

        WithdrawalRequestEntity saved = createWithdrawalEntity(
            currentUser,
            assignedAgent,
            bankAccount,
            null,
            request.amount().trim(),
            bankAccount.getCountry(),
            bankAccount.getAccountName(),
            bankAccount.getBankName(),
            bankAccount.getAccountNumber(),
            normalizeNullable(request.contact()),
            normalizeNullable(request.note())
        );

        String message = withdrawalMessage(saved, false);
        if (request.sendChatMessage() == null || request.sendChatMessage()) {
            persistentSupportService.appendSystemMessage(conversation, message);
        }
        notifyWithdrawal(currentUser, assignedAgent, saved);

        return toItem(saved);
    }

    @Transactional
    public WithdrawalItem createLotteryWithdrawal(String recordId) {
        return createLotteryWithdrawal(recordId, null);
    }

    @Transactional
    public WithdrawalItem createLotteryWithdrawal(String recordId, CreateLotteryCashClaimRequest request) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        if (!"USER".equalsIgnoreCase(currentUser.getRoleCode())) {
            throw new IllegalArgumentException("Only users can request lottery withdrawals");
        }
        LotteryDrawRecordEntity record = lotteryDrawRecordRepository.findById(recordId)
            .orElseThrow(() -> new IllegalArgumentException("Lottery record not found"));
        if (!record.getUser().getId().equals(currentUser.getId())) {
            throw new IllegalArgumentException("Lottery record not accessible");
        }
        if (!"CASH".equalsIgnoreCase(record.getPrize().getPrizeType())) {
            throw new IllegalArgumentException("Physical prizes require delivery details");
        }
        if (withdrawalRequestRepository.existsByLotteryDrawRecord_Id(record.getId())) {
            throw new IllegalArgumentException("Lottery withdrawal request already exists");
        }

        UserBankAccountEntity bankAccount = bankAccountService.findForUser(currentUser).orElse(null);
        if (bankAccount == null) {
            if (request == null || request.bankAccount() == null) {
                throw new IllegalArgumentException("Please bind a bank account first");
            }
            bankAccount = bankAccountService.bindForUser(currentUser, request.bankAccount());
        }
        SupportConversationEntity conversation = persistentSupportService.ensureUserConversation(currentUser);
        UserEntity assignedAgent = conversation == null ? null : conversation.getAssignedAgent();
        if (assignedAgent == null) {
            throw new IllegalArgumentException("No active support agent available");
        }

        WithdrawalRequestEntity saved = createWithdrawalEntity(
            currentUser,
            assignedAgent,
            bankAccount,
            record,
            record.getLocalAmount() == null
                ? record.getPrize().getName()
                : record.getCurrencyCode() + " " + record.getLocalAmount().stripTrailingZeros().toPlainString(),
            bankAccount.getCountry(),
            bankAccount.getAccountName(),
            bankAccount.getBankName(),
            bankAccount.getAccountNumber(),
            currentUser.getPhone(),
            "Lottery prize withdrawal: " + record.getId()
        );
        notifyWithdrawal(currentUser, assignedAgent, saved);
        record.setFulfillmentStatus("PROCESSING");
        record.setProcessedAt(LocalDateTime.now());
        lotteryDrawRecordRepository.save(record);
        persistentSupportService.appendUserOrderMessage(conversation, currentUser, lotteryClaimUserMessage(saved));

        return toItem(saved);
    }

    private WithdrawalRequestEntity createWithdrawalEntity(
        UserEntity owner,
        UserEntity assignedAgent,
        UserBankAccountEntity bankAccount,
        LotteryDrawRecordEntity lotteryDrawRecord,
        String amount,
        String country,
        String accountName,
        String bankName,
        String accountNumber,
        String contact,
        String note
    ) {
        WithdrawalRequestEntity entity = new WithdrawalRequestEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setRequestNo(nextRequestNo());
        entity.setOwnerUser(owner);
        entity.setAssignedAgent(assignedAgent);
        entity.setBankAccount(bankAccount);
        entity.setLotteryDrawRecord(lotteryDrawRecord);
        entity.setSourceType(lotteryDrawRecord == null ? "WALLET" : "LOTTERY_CASH");
        entity.setAmount(amount.trim());
        entity.setCountry(country.trim());
        entity.setAccountName(accountName.trim());
        entity.setBankName(bankName.trim());
        entity.setAccountNumber(accountNumber.trim());
        entity.setContact(contact);
        entity.setNote(note);
        entity.setStatusCode("PENDING");
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return withdrawalRequestRepository.save(entity);
    }

    private UserBankAccountEntity bankAccountForWithdrawal(UserEntity currentUser, BindBankAccountRequest request) {
        UserBankAccountEntity existing = bankAccountService.findForUser(currentUser).orElse(null);
        if (existing == null) {
            return bankAccountService.bindForUser(currentUser, request);
        }
        if (!bankAccountService.matches(existing, request)) {
            throw new IllegalArgumentException("Each user can bind only one bank account");
        }
        return existing;
    }

    private String withdrawalMessage(WithdrawalRequestEntity saved, boolean lotteryWithdrawal) {
        String title = lotteryWithdrawal ? "Lottery withdrawal request %s" : "Withdrawal request %s";
        return """
            %s
            Amount: %s
            Country: %s
            Account: %s
            Bank: %s
            Number: %s
            %s
            """.formatted(
            title.formatted(saved.getRequestNo()),
            saved.getAmount(),
            saved.getCountry(),
            saved.getAccountName(),
            saved.getBankName(),
            saved.getAccountNumber(),
            lotteryWithdrawal && saved.getLotteryDrawRecord() != null ? "Lottery record: " + saved.getLotteryDrawRecord().getId() : ""
        ).trim();
    }

    private String lotteryClaimUserMessage(WithdrawalRequestEntity withdrawal) {
        return "I submitted a lottery cash claim for %s (order %s). Payout account: %s at %s, account number %s."
            .formatted(
                withdrawal.getAmount(),
                withdrawal.getRequestNo(),
                withdrawal.getAccountName(),
                withdrawal.getBankName(),
                withdrawal.getAccountNumber()
            );
    }

    private void notifyWithdrawal(UserEntity currentUser, UserEntity assignedAgent, WithdrawalRequestEntity saved) {
        boolean lotteryClaim = saved.getLotteryDrawRecord() != null;
        String title = lotteryClaim ? "New lottery cash claim" : "New withdrawal request";
        notificationService.notifyUser(
            assignedAgent,
            currentUser,
            "WITHDRAWAL",
            title,
            currentUser.getUsername() + " submitted " + saved.getRequestNo(),
            "WITHDRAWAL",
            saved.getId()
        );
        notificationService.notifyAdmins(
            currentUser,
            "WITHDRAWAL",
            title,
            currentUser.getUsername() + " submitted " + saved.getRequestNo(),
            "WITHDRAWAL",
            saved.getId()
        );
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
        WithdrawalRequestEntity saved = withdrawalRequestRepository.save(entity);
        if ("COMPLETED".equalsIgnoreCase(saved.getStatusCode()) && saved.getLotteryDrawRecord() != null) {
            LotteryDrawRecordEntity record = saved.getLotteryDrawRecord();
            record.setFulfillmentStatus("FULFILLED");
            record.setProcessedBy(currentUser);
            record.setProcessedAt(LocalDateTime.now());
            lotteryDrawRecordRepository.save(record);
        }
        return toItem(saved);
    }

    private WithdrawalItem toItem(WithdrawalRequestEntity entity) {
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
