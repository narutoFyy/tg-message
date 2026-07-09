package com.cardnova.giftchat.controller;

import com.cardnova.giftchat.api.ApiResponse;
import com.cardnova.giftchat.dto.LotterySpinRequest;
import com.cardnova.giftchat.model.LotteryDrawResult;
import com.cardnova.giftchat.model.LotteryEligibility;
import com.cardnova.giftchat.model.LotteryPrizeItem;
import com.cardnova.giftchat.model.LotteryWinnerItem;
import com.cardnova.giftchat.model.WithdrawalItem;
import com.cardnova.giftchat.service.LotteryService;
import com.cardnova.giftchat.service.WithdrawalService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lottery")
public class LotteryController {

    private final LotteryService lotteryService;
    private final WithdrawalService withdrawalService;

    public LotteryController(LotteryService lotteryService, WithdrawalService withdrawalService) {
        this.lotteryService = lotteryService;
        this.withdrawalService = withdrawalService;
    }

    @GetMapping("/eligibility")
    public ApiResponse<LotteryEligibility> eligibility() {
        return ApiResponse.success(lotteryService.currentEligibility());
    }

    @PostMapping("/spin")
    public ApiResponse<LotteryDrawResult> spin(@RequestBody(required = false) LotterySpinRequest request) {
        String prizeName = request == null ? "" : request.prizeName();
        return ApiResponse.success("lottery_drawn", lotteryService.spin(prizeName));
    }

    @GetMapping("/winners")
    public ApiResponse<List<LotteryWinnerItem>> winners() {
        return ApiResponse.success(lotteryService.winners());
    }

    @GetMapping("/prizes")
    public ApiResponse<List<LotteryPrizeItem>> prizes() {
        return ApiResponse.success(lotteryService.prizes());
    }

    @PostMapping("/records/{recordId}/withdrawal-request")
    public ApiResponse<WithdrawalItem> requestLotteryWithdrawal(@PathVariable String recordId) {
        return ApiResponse.success("lottery_withdrawal_requested", withdrawalService.createLotteryWithdrawal(recordId));
    }
}
