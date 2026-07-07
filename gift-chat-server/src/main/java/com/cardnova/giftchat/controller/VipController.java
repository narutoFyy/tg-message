package com.cardnova.giftchat.controller;

import com.cardnova.giftchat.api.ApiResponse;
import com.cardnova.giftchat.model.VipSummary;
import com.cardnova.giftchat.service.VipService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vip")
public class VipController {

    private final VipService vipService;

    public VipController(VipService vipService) {
        this.vipService = vipService;
    }

    @GetMapping("/me")
    public ApiResponse<VipSummary> me() {
        return ApiResponse.success(vipService.currentUserSummary());
    }
}
