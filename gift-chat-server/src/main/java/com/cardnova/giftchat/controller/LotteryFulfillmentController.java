package com.cardnova.giftchat.controller;

import com.cardnova.giftchat.api.ApiResponse;
import com.cardnova.giftchat.dto.UpdateWithdrawalStatusRequest;
import com.cardnova.giftchat.model.LotteryFulfillmentItem;
import com.cardnova.giftchat.service.LotteryFulfillmentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/lottery-fulfillments")
public class LotteryFulfillmentController {

    private final LotteryFulfillmentService fulfillmentService;

    public LotteryFulfillmentController(LotteryFulfillmentService fulfillmentService) {
        this.fulfillmentService = fulfillmentService;
    }

    @GetMapping
    public ApiResponse<List<LotteryFulfillmentItem>> orders() {
        return ApiResponse.success(fulfillmentService.orders());
    }

    @PostMapping("/{orderId}/status")
    public ApiResponse<LotteryFulfillmentItem> updateStatus(
        @PathVariable String orderId,
        @Valid @RequestBody UpdateWithdrawalStatusRequest request
    ) {
        return ApiResponse.success("lottery_fulfillment_status_updated", fulfillmentService.updateStatus(orderId, request.status()));
    }
}
