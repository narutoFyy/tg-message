package com.cardnova.giftchat.controller;

import com.cardnova.giftchat.api.ApiResponse;
import com.cardnova.giftchat.dto.LoginResponse;
import com.cardnova.giftchat.dto.UpdateAvatarRequest;
import com.cardnova.giftchat.service.AccountProfileService;
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

    public AccountController(AccountProfileService accountProfileService) {
        this.accountProfileService = accountProfileService;
    }

    @GetMapping("/me")
    public ApiResponse<LoginResponse> me() {
        return ApiResponse.success(accountProfileService.currentProfile());
    }

    @PostMapping("/avatar")
    public ApiResponse<LoginResponse> updateAvatar(@Valid @RequestBody UpdateAvatarRequest request) {
        return ApiResponse.success("avatar_updated", accountProfileService.updateAvatar(request.avatarUrl()));
    }
}
