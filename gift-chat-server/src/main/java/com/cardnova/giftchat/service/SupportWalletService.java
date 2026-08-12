package com.cardnova.giftchat.service;

import com.cardnova.giftchat.dto.AdjustCustomerWalletRequest;
import com.cardnova.giftchat.dto.UnlockLockedBalanceRequest;
import com.cardnova.giftchat.entity.SupportConversationEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.entity.WalletOperationEntity;
import com.cardnova.giftchat.model.CustomerBalanceSummary;
import com.cardnova.giftchat.repository.UserRepository;
import com.cardnova.giftchat.repository.WalletOperationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class SupportWalletService {

    private final CurrentUserService currentUserService;
    private final PersistentSupportService persistentSupportService;
    private final UserRepository userRepository;
    private final WalletOperationRepository walletOperationRepository;
    private final RegistrationBonusService registrationBonusService;
    private final BalanceService balanceService;

    public SupportWalletService(
        CurrentUserService currentUserService,
        PersistentSupportService persistentSupportService,
        UserRepository userRepository,
        WalletOperationRepository walletOperationRepository,
        RegistrationBonusService registrationBonusService,
        BalanceService balanceService
    ) {
        this.currentUserService = currentUserService;
        this.persistentSupportService = persistentSupportService;
        this.userRepository = userRepository;
        this.walletOperationRepository = walletOperationRepository;
        this.registrationBonusService = registrationBonusService;
        this.balanceService = balanceService;
    }

    @Transactional
    public CustomerBalanceSummary adjust(String conversationId, AdjustCustomerWalletRequest request) {
        UserEntity operator = currentUserService.getCurrentUser();
        currentUserService.requireAgentOrAdmin(operator);
        SupportConversationEntity conversation = persistentSupportService.getAccessibleConversationForStaff(conversationId);
        UserEntity customer = userRepository.findByIdForUpdate(conversation.getCustomerUser().getId())
            .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        BigDecimal amount = request.amount().setScale(2, java.math.RoundingMode.HALF_UP);
        BigDecimal delta = "ADD".equals(request.action()) ? amount : amount.negate();
        if (delta.signum() < 0 && balanceService.availableBalanceForUser(customer).compareTo(amount) < 0) {
            throw new IllegalArgumentException("Subtract amount exceeds available balance");
        }

        WalletOperationEntity operation = new WalletOperationEntity();
        operation.setId(UUID.randomUUID().toString());
        operation.setUser(customer);
        operation.setOperatorUser(operator);
        operation.setActionType("ADD".equals(request.action()) ? "MANUAL_ADD" : "MANUAL_SUBTRACT");
        operation.setAmountDelta(delta);
        operation.setCurrencyCode(customer.getCurrencyCode() == null ? "" : customer.getCurrencyCode());
        operation.setNote(request.reason().trim());
        operation.setCreatedAt(LocalDateTime.now());
        walletOperationRepository.save(operation);
        return balanceService.customerSummary(customer);
    }

    @Transactional
    public CustomerBalanceSummary unlock(String conversationId, UnlockLockedBalanceRequest request) {
        UserEntity operator = currentUserService.getCurrentUser();
        currentUserService.requireAgentOrAdmin(operator);
        SupportConversationEntity conversation = persistentSupportService.getAccessibleConversationForStaff(conversationId);
        UserEntity customer = userRepository.findByIdForUpdate(conversation.getCustomerUser().getId())
            .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        BigDecimal unlockedAmount = registrationBonusService.unlockLockedForSupport(customer, request.reason());
        WalletOperationEntity operation = new WalletOperationEntity();
        operation.setId(UUID.randomUUID().toString());
        operation.setUser(customer);
        operation.setOperatorUser(operator);
        operation.setActionType("LOCKED_BONUS_UNLOCK");
        operation.setAmountDelta(unlockedAmount.setScale(2, java.math.RoundingMode.HALF_UP));
        operation.setCurrencyCode(customer.getCurrencyCode() == null ? "" : customer.getCurrencyCode());
        operation.setReferenceId(customer.getId());
        operation.setNote(StringUtils.hasText(request.reason()) ? request.reason().trim() : "Manual unlock of locked registration balance");
        operation.setCreatedAt(LocalDateTime.now());
        walletOperationRepository.save(operation);
        return balanceService.customerSummary(customer);
    }
}
