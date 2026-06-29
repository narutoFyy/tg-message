package com.cardnova.giftchat.controller;

import com.cardnova.giftchat.api.ApiResponse;
import com.cardnova.giftchat.dto.CreateWithdrawalRequest;
import com.cardnova.giftchat.dto.UpdateWithdrawalStatusRequest;
import com.cardnova.giftchat.model.WithdrawalItem;
import com.cardnova.giftchat.service.WithdrawalService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/withdrawals")
public class WithdrawalController {

    private final WithdrawalService withdrawalService;

    public WithdrawalController(WithdrawalService withdrawalService) {
        this.withdrawalService = withdrawalService;
    }

    @GetMapping
    public ApiResponse<List<WithdrawalItem>> withdrawals() {
        return ApiResponse.success(withdrawalService.getWithdrawals());
    }

    @PostMapping
    public ApiResponse<WithdrawalItem> create(@Valid @RequestBody CreateWithdrawalRequest request) {
        return ApiResponse.success("withdrawal_created", withdrawalService.create(request));
    }

    @PostMapping("/{withdrawalId}/status")
    public ApiResponse<WithdrawalItem> updateStatus(
        @PathVariable String withdrawalId,
        @Valid @RequestBody UpdateWithdrawalStatusRequest request
    ) {
        return ApiResponse.success("withdrawal_status_updated", withdrawalService.updateStatus(withdrawalId, request.status()));
    }
}
