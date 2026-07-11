package com.cardnova.giftchat.controller;

import com.cardnova.giftchat.api.ApiResponse;
import com.cardnova.giftchat.dto.CreateLotteryCashClaimRequest;
import com.cardnova.giftchat.dto.CreateLotteryFulfillmentRequest;
import com.cardnova.giftchat.model.LotteryDrawResult;
import com.cardnova.giftchat.model.LotteryEligibility;
import com.cardnova.giftchat.model.LotteryPrizeItem;
import com.cardnova.giftchat.model.LotteryWinnerItem;
import com.cardnova.giftchat.model.LotteryRecordItem;
import com.cardnova.giftchat.model.LotteryFulfillmentItem;
import com.cardnova.giftchat.model.WithdrawalItem;
import com.cardnova.giftchat.service.LotteryService;
import com.cardnova.giftchat.service.WithdrawalService;
import com.cardnova.giftchat.service.LotteryFulfillmentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/lottery")
public class LotteryController {

    private final LotteryService lotteryService;
    private final WithdrawalService withdrawalService;
    private final LotteryFulfillmentService fulfillmentService;

    public LotteryController(
        LotteryService lotteryService,
        WithdrawalService withdrawalService,
        LotteryFulfillmentService fulfillmentService
    ) {
        this.lotteryService = lotteryService;
        this.withdrawalService = withdrawalService;
        this.fulfillmentService = fulfillmentService;
    }

    @GetMapping("/eligibility")
    public ApiResponse<LotteryEligibility> eligibility() {
        return ApiResponse.success(lotteryService.currentEligibility());
    }

    @PostMapping("/spin")
    public ApiResponse<LotteryDrawResult> spin() {
        return ApiResponse.success("lottery_drawn", lotteryService.spin());
    }

    @GetMapping("/winners")
    public ApiResponse<List<LotteryWinnerItem>> winners() {
        return ApiResponse.success(lotteryService.winners());
    }

    @GetMapping("/prizes")
    public ApiResponse<List<LotteryPrizeItem>> prizes() {
        return ApiResponse.success(lotteryService.prizes());
    }

    @GetMapping("/records/me")
    public ApiResponse<List<LotteryRecordItem>> myRecords() {
        return ApiResponse.success(lotteryService.myRecords());
    }

    @PostMapping("/records/{recordId}/withdrawal-request")
    public ApiResponse<WithdrawalItem> requestLotteryWithdrawal(
        @PathVariable String recordId,
        @Valid @RequestBody(required = false) CreateLotteryCashClaimRequest request
    ) {
        return ApiResponse.success("lottery_withdrawal_requested", withdrawalService.createLotteryWithdrawal(recordId, request));
    }

    @PostMapping("/records/{recordId}/fulfillment-order")
    public ApiResponse<LotteryFulfillmentItem> createFulfillmentOrder(
        @PathVariable String recordId,
        @Valid @RequestBody CreateLotteryFulfillmentRequest request
    ) {
        return ApiResponse.success("lottery_fulfillment_created", fulfillmentService.create(recordId, request));
    }
}
