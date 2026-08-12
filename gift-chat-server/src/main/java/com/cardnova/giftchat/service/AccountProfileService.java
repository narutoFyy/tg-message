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
    private final CountryCodeService countryCodeService;
    private final InviteCodeService inviteCodeService;

    public AccountProfileService(
        CurrentUserService currentUserService,
        UploadAssetRepository uploadAssetRepository,
        UserRepository userRepository,
        JwtService jwtService,
        CountryCodeService countryCodeService,
        InviteCodeService inviteCodeService
    ) {
        this.currentUserService = currentUserService;
        this.uploadAssetRepository = uploadAssetRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.countryCodeService = countryCodeService;
        this.inviteCodeService = inviteCodeService;
    }

    public LoginResponse currentProfile() {
        return toSession(currentUserService.getCurrentUser());
    }

    @Transactional
    public LoginResponse updateAvatar(String avatarUrl) {
        UserEntity user = currentUserService.getCurrentUser();
        String normalizedAvatarUrl = avatarUrl == null ? "" : avatarUrl.trim();
        UploadAssetEntity asset = findUploadAsset(normalizedAvatarUrl)
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

    private java.util.Optional<UploadAssetEntity> findUploadAsset(String avatarUrl) {
        var exact = uploadAssetRepository.findByPublicUrl(avatarUrl);
        if (exact.isPresent()) {
            return exact;
        }

        int uploadPathStart = avatarUrl.indexOf("/uploads/images/");
        if (uploadPathStart < 0) {
            return java.util.Optional.empty();
        }
        String uploadPath = avatarUrl.substring(uploadPathStart);
        int queryStart = uploadPath.indexOf('?');
        int fragmentStart = uploadPath.indexOf('#');
        int pathEnd = uploadPath.length();
        if (queryStart >= 0) {
            pathEnd = Math.min(pathEnd, queryStart);
        }
        if (fragmentStart >= 0) {
            pathEnd = Math.min(pathEnd, fragmentStart);
        }
        uploadPath = uploadPath.substring(0, pathEnd);

        exact = uploadAssetRepository.findByPublicUrl(uploadPath);
        return exact.isPresent() ? exact : uploadAssetRepository.findFirstByPublicUrlEndingWith(uploadPath);
    }

    public LoginResponse toSession(UserEntity user) {
        var country = countryCodeService.findCountry(user.getCountryCode()).orElse(null);
        return new LoginResponse(
            jwtService.issueAccessToken(user),
            user.getUsername(),
            user.getEmail(),
            user.getPhone(),
            user.getAvatarUrl() == null ? "" : user.getAvatarUrl(),
            user.getInviteCode() == null ? "" : user.getInviteCode(),
            inviteCodeService.invitedByLabel(user),
            country == null ? "" : country.code(),
            country == null ? "" : country.countryName(),
            country == null ? "" : country.currencyCode(),
            country == null ? "" : country.currencySymbol(),
            user.getRoleCode(),
            nextRoute(user),
            jwtService.getAccessTokenExpiry(user).toString()
        );
    }

    private String nextRoute(UserEntity user) {
        if ("ADMIN".equalsIgnoreCase(user.getRoleCode())) {
            return "/pages/admin-console/index";
        }
        if ("AGENT".equalsIgnoreCase(user.getRoleCode())) {
            return "/pages/support-chat-v2/index";
        }
        return "/pages/support/index";
    }
}
