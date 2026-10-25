package com.cardnova.giftchat.service;

import com.cardnova.giftchat.api.ForbiddenException;
import com.cardnova.giftchat.api.UnauthorizedException;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AccountDeletionService {
    private final CurrentUserService currentUserService;
    private final UserRepository users;
    private final PasswordService passwords;
    private final LoginRateLimitService rateLimit;
    private final PushDeviceRepository pushDevices;
    private final WebPushSubscriptionRepository webPush;
    private final InviteCodeRepository inviteCodes;
    private final UserBankAccountRepository bankAccounts;
    private final UserPresenceService presence;

    public AccountDeletionService(CurrentUserService currentUserService, UserRepository users,
        PasswordService passwords, LoginRateLimitService rateLimit, PushDeviceRepository pushDevices,
        WebPushSubscriptionRepository webPush, InviteCodeRepository inviteCodes,
        UserBankAccountRepository bankAccounts, UserPresenceService presence) {
        this.currentUserService = currentUserService;
        this.users = users;
        this.passwords = passwords;
        this.rateLimit = rateLimit;
        this.pushDevices = pushDevices;
        this.webPush = webPush;
        this.inviteCodes = inviteCodes;
        this.bankAccounts = bankAccounts;
        this.presence = presence;
    }

    @Transactional
    public void deleteAccount(String password) {
        UserEntity authenticated = currentUserService.getAuthenticatedUser();
        UserEntity user = users.findByIdForUpdate(authenticated.getId())
            .orElseThrow(() -> new UnauthorizedException("Account not found"));
        if (!"ACTIVE".equals(user.getStatusCode()) || user.getDeletedAt() != null)
            throw new UnauthorizedException("Account is not active");
        if (!"USER".equals(user.getRoleCode()))
            throw new ForbiddenException("Staff account closure requires an administrator");
        String rateKey = "delete:" + user.getId();
        rateLimit.checkAllowed(rateKey, "account");
        if (!passwords.matches(password, user.getPasswordHash())) {
            rateLimit.recordFailure(rateKey, "account");
            throw new IllegalArgumentException("Incorrect password. Your account has not been deleted.");
        }

        LocalDateTime now = LocalDateTime.now();
        pushDevices.deleteByUser_Id(user.getId());
        webPush.deleteByUser_Id(user.getId());
        inviteCodes.findByOwnerUser_Id(user.getId()).ifPresent(code -> {
            code.setEnabled(false);
            code.setUpdatedAt(now);
        });
        // Preserve payout foreign keys while removing the saved payee details.
        bankAccounts.findByOwnerUser_Id(user.getId()).ifPresent(bank -> {
            bank.setAccountName("Deleted");
            bank.setBankName("Deleted");
            bank.setAccountNumber("");
            bank.setNormalizedBankName("");
            bank.setNormalizedAccountNumber("");
            bank.setAccountFingerprint("deleted-" + UUID.randomUUID());
            bank.setMaskedAccountNumber("");
            bank.setCountry("");
            bank.setStatusCode("DELETED");
            bank.setUpdatedAt(now);
        });
        user.setUsername("deleted_" + UUID.randomUUID().toString().replace("-", ""));
        user.setEmail(null);
        user.setPhone(null);
        user.setAvatarUrl(null);
        user.setBirthDate(null);
        user.setTencentUserId(null);
        user.setInviteCode(null);
        user.setPasswordHash("deleted");
        user.setStatusCode("DELETED");
        user.setDeletedAt(now);
        user.setUpdatedAt(now);
        users.saveAndFlush(user);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                presence.disconnectUser(user.getId());
            }
        });
    }
}
