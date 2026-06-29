package com.cardnova.giftchat.service;

import com.cardnova.giftchat.dto.CreateBlacklistRequest;
import com.cardnova.giftchat.entity.BlacklistEntryEntity;
import com.cardnova.giftchat.entity.FriendshipEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.model.FriendProfile;
import com.cardnova.giftchat.repository.BlacklistEntryRepository;
import com.cardnova.giftchat.repository.FriendshipRepository;
import com.cardnova.giftchat.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class PersistentBlacklistService {

    private static final DateTimeFormatter BLACKLIST_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final BlacklistEntryRepository blacklistEntryRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public PersistentBlacklistService(
        BlacklistEntryRepository blacklistEntryRepository,
        FriendshipRepository friendshipRepository,
        UserRepository userRepository,
        CurrentUserService currentUserService
    ) {
        this.blacklistEntryRepository = blacklistEntryRepository;
        this.friendshipRepository = friendshipRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    public List<FriendProfile> getBlacklist() {
        String ownerId = currentUserService.getCurrentUser().getId();

        return blacklistEntryRepository.findByOwnerUser_Id(ownerId).stream()
            .map(entry -> new FriendProfile(
                entry.getId(),
                entry.getBlockedUser().getUsername(),
                entry.getBlockedUser().getUsername(),
                entry.getBlockedPhoneSnapshot(),
                "blocked",
                List.of("Blocked from chat"),
                List.of(),
                0,
                BLACKLIST_TIME_FORMATTER.format(entry.getCreatedAt())
            ))
            .toList();
    }

    @Transactional
    public FriendProfile addToBlacklist(CreateBlacklistRequest request) {
        UserEntity owner = currentUserService.getCurrentUser();
        UserEntity blockedUser = userRepository.findByUsername(request.username().trim())
            .orElseThrow(() -> new IllegalArgumentException("User to block not found"));

        if (blacklistEntryRepository.existsByOwnerUser_IdAndBlockedUser_Id(owner.getId(), blockedUser.getId())) {
            throw new IllegalArgumentException("User already blocked");
        }

        BlacklistEntryEntity entity = new BlacklistEntryEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setOwnerUser(owner);
        entity.setBlockedUser(blockedUser);
        entity.setBlockedPhoneSnapshot(blockedUser.getPhone() == null ? "" : blockedUser.getPhone());
        entity.setReasonNote(request.reason() == null ? "Blocked from direct contact." : request.reason().trim());
        entity.setCreatedAt(LocalDateTime.now());
        blacklistEntryRepository.save(entity);

        markFriendshipBlocked(owner.getId(), blockedUser.getId());

        return new FriendProfile(
            entity.getId(),
            blockedUser.getUsername(),
            blockedUser.getUsername(),
            blockedUser.getPhone() == null ? "" : blockedUser.getPhone(),
            "blocked",
            List.of("Blocked from chat"),
            List.of(),
            0,
            BLACKLIST_TIME_FORMATTER.format(entity.getCreatedAt())
        );
    }

    @Transactional
    public FriendProfile removeFromBlacklist(String blacklistId) {
        UserEntity owner = currentUserService.getCurrentUser();
        BlacklistEntryEntity entry = blacklistEntryRepository.findByIdAndOwnerUser_Id(blacklistId, owner.getId())
            .orElseThrow(() -> new IllegalArgumentException("Blacklist entry not found"));

        FriendProfile removed = new FriendProfile(
            entry.getId(),
            entry.getBlockedUser().getUsername(),
            entry.getBlockedUser().getUsername(),
            entry.getBlockedPhoneSnapshot(),
            "online",
            List.of("Unblocked"),
            List.of(),
            0,
            BLACKLIST_TIME_FORMATTER.format(entry.getCreatedAt())
        );
        String blockedUserId = entry.getBlockedUser().getId();
        blacklistEntryRepository.delete(entry);
        restoreBlockedFriendship(owner.getId(), blockedUserId);
        return removed;
    }

    private void markFriendshipBlocked(String ownerId, String blockedUserId) {
        FriendshipEntity forward = friendshipRepository.findByRequesterUser_IdAndAddresseeUser_Id(ownerId, blockedUserId).orElse(null);
        if (forward != null) {
            forward.setStatusCode("BLOCKED");
            forward.setUpdatedAt(LocalDateTime.now());
            friendshipRepository.save(forward);
        }

        FriendshipEntity reverse = friendshipRepository.findByAddresseeUser_IdAndRequesterUser_Id(ownerId, blockedUserId).orElse(null);
        if (reverse != null) {
            reverse.setStatusCode("BLOCKED");
            reverse.setUpdatedAt(LocalDateTime.now());
            friendshipRepository.save(reverse);
        }
    }

    private void restoreBlockedFriendship(String ownerId, String blockedUserId) {
        if (hasBlacklistRelationship(ownerId, blockedUserId)) {
            return;
        }

        FriendshipEntity forward = friendshipRepository.findByRequesterUser_IdAndAddresseeUser_Id(ownerId, blockedUserId).orElse(null);
        if (forward != null && "BLOCKED".equals(forward.getStatusCode())) {
            forward.setStatusCode("ACCEPTED");
            forward.setUpdatedAt(LocalDateTime.now());
            friendshipRepository.save(forward);
        }

        FriendshipEntity reverse = friendshipRepository.findByAddresseeUser_IdAndRequesterUser_Id(ownerId, blockedUserId).orElse(null);
        if (reverse != null && "BLOCKED".equals(reverse.getStatusCode())) {
            reverse.setStatusCode("ACCEPTED");
            reverse.setUpdatedAt(LocalDateTime.now());
            friendshipRepository.save(reverse);
        }
    }

    private boolean hasBlacklistRelationship(String ownerId, String blockedUserId) {
        return blacklistEntryRepository.existsByOwnerUser_IdAndBlockedUser_Id(ownerId, blockedUserId)
            || blacklistEntryRepository.existsByOwnerUser_IdAndBlockedUser_Id(blockedUserId, ownerId);
    }
}
