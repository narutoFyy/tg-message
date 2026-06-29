package com.cardnova.giftchat.controller;

import com.cardnova.giftchat.api.ApiResponse;
import com.cardnova.giftchat.dto.LoginRequest;
import com.cardnova.giftchat.dto.LoginResponse;
import com.cardnova.giftchat.dto.RegisterRequest;
import com.cardnova.giftchat.entity.UserEntity;
import com.cardnova.giftchat.service.CurrentUserService;
import com.cardnova.giftchat.service.PersistentAccountService;
import com.cardnova.giftchat.service.UserPresenceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final PersistentAccountService persistentAccountService;
    private final CurrentUserService currentUserService;
    private final UserPresenceService userPresenceService;

    public AuthController(
        PersistentAccountService persistentAccountService,
        CurrentUserService currentUserService,
        UserPresenceService userPresenceService
    ) {
        this.persistentAccountService = persistentAccountService;
        this.currentUserService = currentUserService;
        this.userPresenceService = userPresenceService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(persistentAccountService.login(request.identifier(), request.password()));
    }

    @PostMapping("/register")
    public ApiResponse<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("registered", persistentAccountService.registerAndLogin(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Boolean> logout() {
        UserEntity currentUser = currentUserService.getCurrentUser();
        userPresenceService.disconnectUser(currentUser.getId());
        return ApiResponse.success("logged_out", true);
    }
}
