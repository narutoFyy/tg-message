package com.cardnova.giftchat.service;

import com.cardnova.giftchat.dto.BindBankAccountRequest;
import com.cardnova.giftchat.entity.UserBankAccountEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.model.BankAccountItem;
import com.cardnova.giftchat.repository.UserBankAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class BankAccountService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UserBankAccountRepository userBankAccountRepository;
    private final CurrentUserService currentUserService;

    public BankAccountService(
        UserBankAccountRepository userBankAccountRepository,
        CurrentUserService currentUserService
    ) {
        this.userBankAccountRepository = userBankAccountRepository;
        this.currentUserService = currentUserService;
    }

    public BankAccountItem myBankAccount() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        return userBankAccountRepository.findByOwnerUser_Id(currentUser.getId())
            .map(account -> toItem(account, true))
            .orElse(null);
    }

    public List<BankAccountItem> adminBankAccounts() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        currentUserService.requireAdmin(currentUser);
        return userBankAccountRepository.findAllByOrderByCreatedAtDesc().stream()
            .map(account -> toItem(account, false))
            .toList();
    }

    @Transactional
    public BankAccountItem bind(BindBankAccountRequest request) {
        UserEntity currentUser = currentUserService.getCurrentUser();
        if (!"USER".equalsIgnoreCase(currentUser.getRoleCode())) {
            throw new IllegalArgumentException("Only users can bind bank accounts");
        }
        return toItem(bindForUser(currentUser, request), true);
    }

    @Transactional
    public UserBankAccountEntity bindForUser(UserEntity user, BindBankAccountRequest request) {
        if (userBankAccountRepository.existsByOwnerUser_Id(user.getId())) {
            throw new IllegalArgumentException("Each user can bind only one bank account");
        }

        String country = requireTrimmed(request.country(), "Country is required");
        String accountName = requireTrimmed(request.accountName(), "Account name is required");
        String bankName = requireTrimmed(request.bankName(), "Bank name is required");
        String accountNumber = requireTrimmed(request.accountNumber(), "Account number is required");
        String normalizedBank = normalizeBankName(bankName);
        String normalizedAccount = normalizeAccountNumber(accountNumber);
        if (!StringUtils.hasText(normalizedAccount)) {
            throw new IllegalArgumentException("Account number is required");
        }
        String fingerprint = fingerprint(country, normalizedBank, normalizedAccount);
        if (userBankAccountRepository.existsByAccountFingerprint(fingerprint)) {
            throw new IllegalArgumentException("This bank account is already bound");
        }

        LocalDateTime now = LocalDateTime.now();
        UserBankAccountEntity account = new UserBankAccountEntity();
        account.setId(UUID.randomUUID().toString());
        account.setOwnerUser(user);
        account.setCountry(country);
        account.setAccountName(accountName);
        account.setBankName(bankName);
        account.setAccountNumber(accountNumber);
        account.setNormalizedBankName(normalizedBank);
        account.setNormalizedAccountNumber(normalizedAccount);
        account.setAccountFingerprint(fingerprint);
        account.setMaskedAccountNumber(maskAccountNumber(normalizedAccount));
        account.setStatusCode("ACTIVE");
        account.setCreatedAt(now);
        account.setUpdatedAt(now);
        try {
            return userBankAccountRepository.saveAndFlush(account);
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("This bank account is already bound");
        }
    }

    public UserBankAccountEntity requireCurrentUserBankAccount(UserEntity user) {
        return userBankAccountRepository.findByOwnerUser_Id(user.getId())
            .orElseThrow(() -> new IllegalArgumentException("Please bind a bank account first"));
    }

    public Optional<UserBankAccountEntity> findForUser(UserEntity user) {
        return userBankAccountRepository.findByOwnerUser_Id(user.getId());
    }

    public boolean matches(UserBankAccountEntity account, BindBankAccountRequest request) {
        if (account == null || request == null) {
            return false;
        }
        return normalizeText(account.getCountry()).equals(normalizeText(request.country()))
            && account.getNormalizedBankName().equals(normalizeBankName(request.bankName()))
            && account.getNormalizedAccountNumber().equals(normalizeAccountNumber(request.accountNumber()));
    }

    private BankAccountItem toItem(UserBankAccountEntity account, boolean includeFullAccountNumber) {
        return new BankAccountItem(
            account.getId(),
            account.getOwnerUser().getUsername(),
            account.getCountry(),
            account.getAccountName(),
            account.getBankName(),
            includeFullAccountNumber ? account.getAccountNumber() : "",
            account.getMaskedAccountNumber(),
            account.getStatusCode().toLowerCase(Locale.ROOT),
            TIME_FORMATTER.format(account.getCreatedAt())
        );
    }

    private String requireTrimmed(String value, String message) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }

    private String normalizeBankName(String value) {
        return normalizeText(value).replaceAll("[^a-z0-9]", "");
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeAccountNumber(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
    }

    private String fingerprint(String country, String normalizedBank, String normalizedAccount) {
        String raw = normalizeText(country).replaceAll("[^a-z0-9]", "") + "|" + normalizedBank + "|" + normalizedAccount;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String maskAccountNumber(String normalizedAccountNumber) {
        if (normalizedAccountNumber.length() <= 4) {
            return "****";
        }
        return "****" + normalizedAccountNumber.substring(normalizedAccountNumber.length() - 4);
    }
}
