package com.cardnova.giftchat.controller;

import com.cardnova.giftchat.api.ApiResponse;
import com.cardnova.giftchat.dto.LoginResponse;
import com.cardnova.giftchat.dto.UpdateAvatarRequest;
import com.cardnova.giftchat.model.RegistrationBonusRecordItem;
import com.cardnova.giftchat.service.AccountProfileService;
import com.cardnova.giftchat.service.RegistrationBonusService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final AccountProfileService accountProfileService;
    private final RegistrationBonusService registrationBonusService;

    public AccountController(AccountProfileService accountProfileService, RegistrationBonusService registrationBonusService) {
        this.accountProfileService = accountProfileService;
        this.registrationBonusService = registrationBonusService;
    }

    @GetMapping("/me")
    public ApiResponse<LoginResponse> me() {
        return ApiResponse.success(accountProfileService.currentProfile());
    }

    @PostMapping("/avatar")
    public ApiResponse<LoginResponse> updateAvatar(@Valid @RequestBody UpdateAvatarRequest request) {
        return ApiResponse.success("avatar_updated", accountProfileService.updateAvatar(request.avatarUrl()));
    }

    @GetMapping("/registration-bonus")
    public ApiResponse<RegistrationBonusRecordItem> registrationBonus() {
        return ApiResponse.success(registrationBonusService.currentUserRecord());
    }
}
