package com.cardnova.giftchat.service;

import com.cardnova.giftchat.dto.LoginResponse;
import com.cardnova.giftchat.entity.UploadAssetEntity;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.repository.UploadAssetRepository;
import com.cardnova.giftchat.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AccountProfileService {

    private final CurrentUserService currentUserService;
    private final UploadAssetRepository uploadAssetRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public AccountProfileService(
        CurrentUserService currentUserService,
        UploadAssetRepository uploadAssetRepository,
        UserRepository userRepository,
        JwtService jwtService
    ) {
        this.currentUserService = currentUserService;
        this.uploadAssetRepository = uploadAssetRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    public LoginResponse currentProfile() {
        return toSession(currentUserService.getCurrentUser());
    }

    @Transactional
    public LoginResponse updateAvatar(String avatarUrl) {
        UserEntity user = currentUserService.getCurrentUser();
        String normalizedAvatarUrl = avatarUrl == null ? "" : avatarUrl.trim();
        UploadAssetEntity asset = uploadAssetRepository.findByPublicUrl(normalizedAvatarUrl)
            .orElseThrow(() -> new IllegalArgumentException("Avatar image not found"));
        if (!asset.getOwnerUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Avatar image is not owned by current user");
        }
        if (asset.getMimeType() == null || !asset.getMimeType().startsWith("image/")) {
            throw new IllegalArgumentException("Avatar must be an image");
        }

        user.setAvatarUrl(asset.getPublicUrl());
        user.setUpdatedAt(LocalDateTime.now());
        return toSession(userRepository.save(user));
    }

    public LoginResponse toSession(UserEntity user) {
        return new LoginResponse(
            jwtService.issueAccessToken(user),
            user.getUsername(),
            user.getEmail(),
            user.getPhone(),
            user.getAvatarUrl() == null ? "" : user.getAvatarUrl(),
            user.getInviteCode() == null ? "" : user.getInviteCode(),
            user.getRoleCode(),
            nextRoute(user),
            jwtService.getAccessTokenExpiry().toString()
        );
    }

    private String nextRoute(UserEntity user) {
        if ("ADMIN".equalsIgnoreCase(user.getRoleCode())) {
            return "/pages/admin-rates/index";
        }
        if ("AGENT".equalsIgnoreCase(user.getRoleCode())) {
            return "/pages/support-chat-v2/index";
        }
        return "/pages/support/index";
    }
}
