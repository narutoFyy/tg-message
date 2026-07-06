package com.cardnova.giftchat.service;

import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.entity.WithdrawalRequestEntity;
import com.cardnova.giftchat.model.BankAccountRiskMatch;
import com.cardnova.giftchat.repository.SupportConversationRepository;
import com.cardnova.giftchat.repository.WithdrawalRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class BankAccountRiskService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final WithdrawalRequestRepository withdrawalRequestRepository;
    private final SupportConversationRepository supportConversationRepository;
    private final PhoneCountryCodeResolver phoneCountryCodeResolver;
    private final RegistrationBonusService registrationBonusService;

    public BankAccountRiskService(
        WithdrawalRequestRepository withdrawalRequestRepository,
        SupportConversationRepository supportConversationRepository,
        PhoneCountryCodeResolver phoneCountryCodeResolver,
        RegistrationBonusService registrationBonusService
    ) {
        this.withdrawalRequestRepository = withdrawalRequestRepository;
        this.supportConversationRepository = supportConversationRepository;
        this.phoneCountryCodeResolver = phoneCountryCodeResolver;
        this.registrationBonusService = registrationBonusService;
    }

    public List<BankAccountRiskMatch> matchesForCustomer(UserEntity customer, UserEntity viewer) {
        List<WithdrawalRequestEntity> currentWithdrawals = withdrawalRequestRepository.findByOwnerUser_IdOrderByUpdatedAtDesc(customer.getId());
        if (currentWithdrawals.isEmpty()) {
            return List.of();
        }

        List<WithdrawalRequestEntity> allWithdrawals = withdrawalRequestRepository.findAllByOrderByUpdatedAtDesc();
        return allWithdrawals.stream()
            .filter(candidate -> !candidate.getOwnerUser().getId().equals(customer.getId()))
            .map(candidate -> bestMatch(currentWithdrawals, candidate, viewer))
            .filter(match -> match != null)
            .distinct()
            .sorted(Comparator.comparing(BankAccountRiskMatch::riskLevel).thenComparing(BankAccountRiskMatch::submittedAt).reversed())
            .limit(12)
            .toList();
    }

    public List<BankAccountRiskMatch> allPlatformMatches(UserEntity viewer) {
        List<WithdrawalRequestEntity> withdrawals = withdrawalRequestRepository.findAllByOrderByUpdatedAtDesc();
        Map<String, List<WithdrawalRequestEntity>> byAccountNumber = new LinkedHashMap<>();
        Map<String, List<WithdrawalRequestEntity>> byNameAndBank = new LinkedHashMap<>();

        for (WithdrawalRequestEntity withdrawal : withdrawals) {
            String accountNumber = normalizeAccountNumber(withdrawal.getAccountNumber());
            if (StringUtils.hasText(accountNumber)) {
                byAccountNumber.computeIfAbsent(accountNumber, ignored -> new ArrayList<>()).add(withdrawal);
            }

            String accountName = normalizeText(withdrawal.getAccountName());
            String bankName = normalizeText(withdrawal.getBankName());
            if (StringUtils.hasText(accountName) && StringUtils.hasText(bankName)) {
                byNameAndBank.computeIfAbsent(accountName + "|" + bankName, ignored -> new ArrayList<>()).add(withdrawal);
            }
        }

        List<BankAccountRiskMatch> matches = new ArrayList<>();
        Set<String> included = new HashSet<>();
        appendDuplicateGroups(matches, included, byAccountNumber, viewer, "high", "Duplicate bank account number");
        appendDuplicateGroups(matches, included, byNameAndBank, viewer, "medium", "Duplicate account name and bank name");
        return matches.stream()
            .sorted(Comparator.comparing(BankAccountRiskMatch::riskLevel).thenComparing(BankAccountRiskMatch::submittedAt).reversed())
            .limit(100)
            .toList();
    }

    private void appendDuplicateGroups(
        List<BankAccountRiskMatch> matches,
        Set<String> included,
        Map<String, List<WithdrawalRequestEntity>> groups,
        UserEntity viewer,
        String level,
        String reason
    ) {
        groups.values().stream()
            .filter(group -> group.stream().map(item -> item.getOwnerUser().getId()).distinct().count() > 1)
            .flatMap(List::stream)
            .filter(withdrawal -> included.add(level + ":" + withdrawal.getId()))
            .map(withdrawal -> toMatch(withdrawal, viewer, level, reason))
            .forEach(matches::add);
    }

    private BankAccountRiskMatch bestMatch(List<WithdrawalRequestEntity> currentWithdrawals, WithdrawalRequestEntity candidate, UserEntity viewer) {
        for (WithdrawalRequestEntity current : currentWithdrawals) {
            String currentNumber = normalizeAccountNumber(current.getAccountNumber());
            String candidateNumber = normalizeAccountNumber(candidate.getAccountNumber());
            String currentName = normalizeText(current.getAccountName());
            String candidateName = normalizeText(candidate.getAccountName());
            String currentBank = normalizeText(current.getBankName());
            String candidateBank = normalizeText(candidate.getBankName());

            if (StringUtils.hasText(currentNumber) && currentNumber.equals(candidateNumber)) {
                String reason = currentName.equals(candidateName) ? "Bank account number and account name match" : "Bank account number matches";
                return toMatch(candidate, viewer, "high", reason);
            }
            if (StringUtils.hasText(currentName) && currentName.equals(candidateName) && StringUtils.hasText(currentBank) && currentBank.equals(candidateBank)) {
                return toMatch(candidate, viewer, "medium", "Account name and bank name match");
            }
        }
        return null;
    }

    private BankAccountRiskMatch toMatch(WithdrawalRequestEntity withdrawal, UserEntity viewer, String level, String reason) {
        UserEntity matchedUser = withdrawal.getOwnerUser();
        boolean admin = "ADMIN".equalsIgnoreCase(viewer.getRoleCode());
        boolean ownCustomer = withdrawal.getAssignedAgent() != null && withdrawal.getAssignedAgent().getId().equals(viewer.getId());
        boolean fullAccess = admin || ownCustomer;
        String username = fullAccess ? matchedUser.getUsername() : mask(matchedUser.getUsername());
        return new BankAccountRiskMatch(
            level,
            reason,
            username,
            username,
            phoneCountryCodeResolver.resolve(matchedUser.getPhone(), registrationBonusService.configuredCountryCodes()),
            withdrawal.getAssignedAgent() == null ? "" : withdrawal.getAssignedAgent().getUsername(),
            fullAccess ? withdrawal.getBankName() : "Restricted",
            fullAccess ? withdrawal.getAccountName() : mask(withdrawal.getAccountName()),
            maskAccountNumber(withdrawal.getAccountNumber()),
            TIME_FORMATTER.format(withdrawal.getUpdatedAt()),
            fullAccess
        );
    }

    private String normalizeAccountNumber(String value) {
        return value == null ? "" : value.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]", "").toUpperCase();
    }

    private String maskAccountNumber(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String normalized = value.trim();
        int length = normalized.length();
        if (length <= 4) {
            return "****";
        }
        return "****" + normalized.substring(length - 4);
    }

    private String mask(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 2) {
            return trimmed.charAt(0) + "*";
        }
        return trimmed.charAt(0) + "***" + trimmed.charAt(trimmed.length() - 1);
    }
}
