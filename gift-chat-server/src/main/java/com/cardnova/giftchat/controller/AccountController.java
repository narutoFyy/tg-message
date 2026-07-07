package com.cardnova.giftchat.controller;

import com.cardnova.giftchat.api.ApiResponse;
import com.cardnova.giftchat.dto.HideRecordRequest;
import com.cardnova.giftchat.dto.LoginResponse;
import com.cardnova.giftchat.dto.UpdateAvatarRequest;
import com.cardnova.giftchat.model.HiddenRecordItem;
import com.cardnova.giftchat.model.RegistrationBonusRecordItem;
import com.cardnova.giftchat.service.AccountProfileService;
import com.cardnova.giftchat.service.RegistrationBonusService;
import com.cardnova.giftchat.service.UserHiddenRecordService;
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
    private final UserHiddenRecordService userHiddenRecordService;

    public AccountController(
        AccountProfileService accountProfileService,
        RegistrationBonusService registrationBonusService,
        UserHiddenRecordService userHiddenRecordService
    ) {
        this.accountProfileService = accountProfileService;
        this.registrationBonusService = registrationBonusService;
        this.userHiddenRecordService = userHiddenRecordService;
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

    @PostMapping("/hidden-records")
    public ApiResponse<HiddenRecordItem> hideRecord(@Valid @RequestBody HideRecordRequest request) {
        return ApiResponse.success(
            "record_hidden",
            userHiddenRecordService.hideCurrentUserRecord(request.targetType(), request.targetId(), request.hiddenScope())
        );
    }

    @GetMapping("/hidden-records")
    public ApiResponse<java.util.List<HiddenRecordItem>> hiddenRecords() {
        return ApiResponse.success(userHiddenRecordService.currentUserRecords());
    }
}
